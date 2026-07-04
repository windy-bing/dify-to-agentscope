package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.trace.ExecutionTracer;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeAgentFactory;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeStreamingAgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeWorkflowPlanMapper;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.dify.WorkflowPlanWriter;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * code-first workflow 运行时注册服务。
 * <p>业务方在 Java 中构建 {@link CodeFirstWorkflowDefinition} 后，通过该服务注册到现有
 * {@link WorkflowRegistry}。注册后的 workflow 与 Dify/YAML 导入 workflow 共享 HTTP、A2A、MCP、知识库、
 * Memos、Trace、节点超时和安全边界。</p>
 */
@Service
public class CodeFirstWorkflowRuntime {

    private final WorkflowRegistry workflowRegistry;
    private final DifyRuntimeProperties properties;
    private final ToolGateway toolGateway;
    private final KnowledgeRetriever knowledgeRetriever;
    private final PermissionPolicy permissionPolicy;
    private final OutputSanitizer outputSanitizer;
    private final ExecutionTracer executionTracer;
    private final MemoStore memoStore;
    private final AgentStateStore agentStateStore;
    private final WorkflowPlanWriter writer = new WorkflowPlanWriter();
    private final AgentScopeWorkflowPlanMapper agentScopeMapper = new AgentScopeWorkflowPlanMapper();

    /**
     * 创建 code-first workflow 运行时注册服务。
     *
     * @param workflowRegistry   workflow 注册表
     * @param properties         全局运行时配置
     * @param toolGateway        工具网关
     * @param knowledgeRetriever 知识库检索器
     * @param permissionPolicy   权限策略
     * @param outputSanitizer    输出净化器
     * @param executionTracer    执行追踪器
     * @param memoStore          长期记忆存储
     * @param agentStateStore    AgentScope 官方状态存储
     */
    public CodeFirstWorkflowRuntime(
            WorkflowRegistry workflowRegistry,
            DifyRuntimeProperties properties,
            ToolGateway toolGateway,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer executionTracer,
            MemoStore memoStore,
            AgentStateStore agentStateStore) {
        this.workflowRegistry = workflowRegistry;
        this.properties = properties;
        this.toolGateway = toolGateway;
        this.knowledgeRetriever = knowledgeRetriever;
        this.permissionPolicy = permissionPolicy;
        this.outputSanitizer = outputSanitizer;
        this.executionTracer = executionTracer;
        this.memoStore = memoStore;
        this.agentStateStore = agentStateStore;
    }

    /**
     * 注册 code-first workflow。
     *
     * @param definition code-first workflow 定义
     * @return 注册后的运行时包
     */
    public WorkflowRuntimeBundle register(CodeFirstWorkflowDefinition definition) {
        WorkflowPlan plan = definition.plan();
        writeAuditArtifact(definition.workflowId(), plan);
        WorkflowRuntimeBundle bundle = new WorkflowRuntimeBundle(
                definition.workflowId(),
                plan,
                agentScopeMapper.toAgentScope(plan),
                executor(definition.workflowId(), plan));
        workflowRegistry.register(definition.workflowId(), bundle);
        return bundle;
    }

    /**
     * 通过 builder 构建并注册 code-first workflow。
     *
     * @param builder code-first workflow 构建器
     * @return 注册后的运行时包
     */
    public WorkflowRuntimeBundle register(CodeFirstWorkflowBuilder builder) {
        return register(builder.build());
    }

    /**
     * 创建 workflow 执行器。
     *
     * @param workflowId workflow ID
     * @param plan       工作流计划
     * @return workflow 执行器
     */
    private WorkflowExecutor executor(String workflowId, WorkflowPlan plan) {
        return new WorkflowExecutor(
                workflowId,
                plan,
                agentInvoker(plan),
                knowledgeRetriever,
                memoStore,
                permissionPolicy,
                outputSanitizer,
                executionTracer,
                properties.getExecution().getMaxSteps(),
                properties.getExecution().getDefaultNodeTimeout(),
                properties.getExecution().getNodeTimeouts(),
                properties.getExecution().getFallbackAnswer(),
                properties.getExecution().getIdentifierRules().getDemoItem(),
                properties.getExecution().getIdentifierRules().getDemoRequest(),
                properties.getExecution().getIdentifierRules().getDemoTask());
    }

    /**
     * 根据运行配置创建 Agent 调用器。
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
     * 写出 code-first workflow 审计产物。
     *
     * @param workflowId workflow ID
     * @param plan       工作流计划
     */
    private void writeAuditArtifact(String workflowId, WorkflowPlan plan) {
        try {
            writer.write(plan, Path.of(properties.getGeneratedOutputDir()).resolve(workflowId));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write code-first workflow artifact", e);
        }
    }
}
