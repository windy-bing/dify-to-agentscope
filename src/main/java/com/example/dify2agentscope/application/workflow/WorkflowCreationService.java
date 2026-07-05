package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.adapter.in.web.CreateAgentWorkflowRequest;
import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.trace.ExecutionTracer;
import com.example.dify2agentscope.domain.workflow.WorkflowDefinitionStore;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeAgentFactory;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeStreamingAgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeWorkflowPlanMapper;
import com.example.dify2agentscope.infrastructure.dify.WorkflowPlanWriter;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;

/**
 * 运行期 workflow 创建服务。
 * <p>用于处理“没有 YAML/DSL 时从零创建”的模板请求，并把生成的 workflow 立即注册到运行时。</p>
 */
@Service
public class WorkflowCreationService {

    private final WorkflowRegistry workflowRegistry;
    private final DifyRuntimeProperties properties;
    private final ToolGateway toolGateway;
    private final KnowledgeRetriever knowledgeRetriever;
    private final PermissionPolicy permissionPolicy;
    private final OutputSanitizer outputSanitizer;
    private final ExecutionTracer executionTracer;
    private final MemoStore memoStore;
    private final AgentStateStore agentStateStore;
    private final ExecutorService nodeExecutor;
    private final WorkflowDefinitionStore workflowDefinitionStore;
    private final WorkflowPlanWriter writer = new WorkflowPlanWriter();
    private final AgentScopeWorkflowPlanMapper agentScopeMapper = new AgentScopeWorkflowPlanMapper();

    /**
     * 测试专用无参构造器。
     * <p>仅用于 Web 层切片测试通过匿名子类替换真实创建逻辑，生产运行使用全参构造器。</p>
     */
    protected WorkflowCreationService() {
        this.workflowRegistry = null;
        this.properties = null;
        this.toolGateway = null;
        this.knowledgeRetriever = null;
        this.permissionPolicy = null;
        this.outputSanitizer = null;
        this.executionTracer = null;
        this.memoStore = null;
        this.agentStateStore = null;
        this.nodeExecutor = null;
        this.workflowDefinitionStore = null;
    }

    /**
     * 创建 workflow 创建服务。
     *
     * @param workflowRegistry  workflow 注册表
     * @param properties        全局运行时配置
     * @param toolGateway       工具网关
     * @param knowledgeRetriever 知识库检索器
     * @param permissionPolicy  权限策略
     * @param outputSanitizer   输出净化器
     * @param executionTracer   执行追踪器
     * @param memoStore         长期记忆存储
     * @param agentStateStore   AgentScope 官方状态存储
     * @param nodeExecutor      workflow 节点共享执行池
     */
    public WorkflowCreationService(
            WorkflowRegistry workflowRegistry,
            DifyRuntimeProperties properties,
            ToolGateway toolGateway,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer executionTracer,
            MemoStore memoStore,
            AgentStateStore agentStateStore,
            ExecutorService nodeExecutor,
            WorkflowDefinitionStore workflowDefinitionStore) {
        this.workflowRegistry = workflowRegistry;
        this.properties = properties;
        this.toolGateway = toolGateway;
        this.knowledgeRetriever = knowledgeRetriever;
        this.permissionPolicy = permissionPolicy;
        this.outputSanitizer = outputSanitizer;
        this.executionTracer = executionTracer;
        this.memoStore = memoStore;
        this.agentStateStore = agentStateStore;
        this.nodeExecutor = nodeExecutor;
        this.workflowDefinitionStore = workflowDefinitionStore;
    }

    /**
     * 从 HTTP 模板请求创建最小可运行的 Agent 工作流，并注册到运行时。
     *
     * @param request 创建工作流请求
     * @return 注册后的工作流运行时包
     */
    public WorkflowRuntimeBundle createAgentWorkflow(CreateAgentWorkflowRequest request) {
        boolean reactMode = isReactMode(request.getExecutionMode());
        WorkflowBuilder builder = reactMode
                ? WorkflowBuilder.createReactSelfLoop(request.getName())
                : WorkflowBuilder.create(request.getName());
        request.getVariables().forEach((name, value) -> builder.variable(name, inferType(value), value, ""));
        if (request.isMemoRetrieval()) {
            builder.memoRetrieval("memos", "Memos", properties.getMemory().getMemos().getDefaultLimit());
        }
        builder.agent(request.getAgentId(), request.getAgentTitle(), request.getInstruction(), request.getModel());
        if (!reactMode) {
            builder.answer("{{#" + request.getAgentId() + ".text#}}");
        }
        WorkflowPlan plan = builder.build();
        workflowDefinitionStore.save(request.getWorkflowId(), plan);
        writeAuditArtifact(request.getWorkflowId(), plan);
        WorkflowExecutor executor = new WorkflowExecutor(
                request.getWorkflowId(),
                plan,
                agentInvoker(plan),
                knowledgeRetriever,
                memoStore,
                permissionPolicy,
                outputSanitizer,
                executionTracer,
                nodeExecutor,
                properties.getExecution().getMaxSteps(),
                properties.getExecution().getDefaultNodeTimeout(),
                properties.getExecution().getNodeTimeouts(),
                properties.getExecution().getFallbackAnswer(),
                properties.getExecution().getIdentifierRules().getDemoItem(),
                properties.getExecution().getIdentifierRules().getDemoRequest(),
                properties.getExecution().getIdentifierRules().getDemoTask());
        WorkflowRuntimeBundle bundle = new WorkflowRuntimeBundle(
                request.getWorkflowId(),
                plan,
                agentScopeMapper.toAgentScope(plan),
                executor);
        workflowRegistry.register(request.getWorkflowId(), bundle);
        return bundle;
    }

    /**
     * 写出动态创建 workflow 的审计产物。
     *
     * @param workflowId workflow ID
     * @param plan       工作流计划
     */
    private void writeAuditArtifact(String workflowId, WorkflowPlan plan) {
        if (!properties.getDeployment().isWriteGeneratedArtifacts()) {
            return;
        }
        try {
            writer.write(plan, Path.of(properties.getGeneratedOutputDir()).resolve(workflowId));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write generated workflow", e);
        }
    }

    /**
     * 判断请求是否要求创建 AgentScope ReAct 自循环 workflow。
     *
     * @param executionMode 请求中的执行模式
     * @return true 表示创建 ReAct 自循环计划
     */
    private boolean isReactMode(String executionMode) {
        return "react".equalsIgnoreCase(executionMode)
                || "agentscope-react".equalsIgnoreCase(executionMode)
                || "react-self-loop".equalsIgnoreCase(executionMode);
    }

    /**
     * 根据配置选择真实 AgentScope 调用器或 Stub 调用器。
     *
     * @param plan 工作流计划
     * @return Agent 调用器
     */
    private AgentInvoker agentInvoker(WorkflowPlan plan) {
        if (!properties.isBuildAgents()) {
            return new StubAgentInvoker();
        }
        Map<String, ReActAgent> agents = new AgentScopeAgentFactory(
                permissionPolicy,
                agentStateStore).buildAgents(plan);
        return new AgentScopeStreamingAgentInvoker(agents, toolGateway);
    }

    /**
     * 根据 Java 默认值推断 workflow 变量类型。
     *
     * @param value 默认值
     * @return workflow 变量类型
     */
    private String inferType(Object value) {
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Iterable<?>) {
            return "array";
        }
        if (value instanceof Map<?, ?>) {
            return "object";
        }
        return "string";
    }
}
