package com.example.dify2agentscope.config;

import com.example.dify2agentscope.application.workflow.WorkflowExecutor;
import com.example.dify2agentscope.application.workflow.WorkflowRegistry;
import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.memory.SessionMemoryStore;
import com.example.dify2agentscope.domain.nacos.NacosResourceCatalog;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.trace.ExecutionTracer;
import com.example.dify2agentscope.domain.workflow.RuntimeConfig;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeAgentFactory;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeStreamingAgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeWorkflowPlanMapper;
import com.example.dify2agentscope.infrastructure.dify.DifyDslParser;
import com.example.dify2agentscope.infrastructure.dify.WorkflowPlanWriter;
import com.example.dify2agentscope.infrastructure.workflow.AgentScopeWorkflowParser;
import com.example.dify2agentscope.infrastructure.knowledge.HttpKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.mcp.HigressMcpToolGateway;
import com.example.dify2agentscope.infrastructure.mcp.StubToolGateway;
import com.example.dify2agentscope.infrastructure.memory.InMemorySessionMemoryStore;
import com.example.dify2agentscope.infrastructure.memory.MemosMemoStore;
import com.example.dify2agentscope.infrastructure.memory.NoOpMemoStore;
import com.example.dify2agentscope.infrastructure.memory.NoOpSessionMemoryStore;
import com.example.dify2agentscope.infrastructure.nacos.NacosResourceSynchronizer;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Dify → AgentScope 工作流运行时配置类。
 * <p>
 * Spring {@link Configuration} 类，负责装配工作流运行时所需的基础设施 Bean，
 * 包括工具网关（ToolGateway）、知识库检索器（KnowledgeRetriever）、安全策略
 * （PermissionPolicy / OutputSanitizer）、Nacos 资源目录、执行追踪器，
 * 以及最终的 {@link WorkflowRegistry}（注册所有已配置的工作流）。
 * <p>
 * 大部分 Bean 依赖 {@link DifyRuntimeProperties} 中的开关与端点配置；
 * 当配置缺失时，根据 {@code failOnMissingIntegrations} 和
 * {@code allowStubIntegrations} 决定是抛出异常还是使用 Stub 实现。
 */
@Configuration
public class WorkflowRuntimeConfig {

    /**
     * 创建运行时配置对象。
     * <p>
     * 将 {@link DifyRuntimeProperties} 中与外部服务通信相关的属性
     * （MCP 端点、知识库端点、请求超时、附加请求头）转换为
     * {@link RuntimeConfig} 值对象，供下游基础设施 Bean 使用。
     *
     * @param properties 全局运行时属性 / global runtime properties
     * @return 运行时配置 / runtime configuration
     */
    @Bean
    RuntimeConfig runtimeConfig(DifyRuntimeProperties properties) {
        return new RuntimeConfig(
                Optional.ofNullable(properties.getHigressMcpEndpoint()),
                Optional.ofNullable(blankToNull(properties.getHigressMcpBearerToken())),
                Optional.ofNullable(properties.getKnowledgeEndpoint()),
                Optional.ofNullable(blankToNull(properties.getKnowledgeBearerToken())),
                properties.getRequestTimeout(),
                properties.getExecution().getDefaultNodeTimeout(),
                properties.getExecution().getNodeTimeouts(),
                properties.getExtraHeaders());
    }

    /**
     * 创建工具网关。
     * <p>
     * 若配置了 Higress MCP 端点则返回 {@link HigressMcpToolGateway}，
     * 否则根据桩实现开关返回 {@link StubToolGateway} 或抛出异常。
     *
     * @param runtimeConfig 运行时配置 / runtime configuration
     * @param properties    全局运行时属性 / global runtime properties
     * @return 工具网关实例 / tool gateway instance
     */
    @Bean
    ToolGateway toolGateway(RuntimeConfig runtimeConfig, DifyRuntimeProperties properties) {
        if (runtimeConfig.higressMcpEndpoint().isPresent()) {
            return new HigressMcpToolGateway(runtimeConfig);
        }
        if (!properties.getBoundary().isAllowStubIntegrations() || properties.isFailOnMissingIntegrations()) {
            throw new IllegalStateException("Higress MCP endpoint is required");
        }
        return new StubToolGateway();
    }

    /**
     * 创建知识库检索器。
     * <p>
     * 若配置了知识库 HTTP 端点则返回 {@link HttpKnowledgeRetriever}，
     * 否则根据桩实现开关返回 {@link StubKnowledgeRetriever} 或抛出异常。
     *
     * @param runtimeConfig 运行时配置 / runtime configuration
     * @param properties    全局运行时属性 / global runtime properties
     * @return 知识库检索器实例 / knowledge retriever instance
     */
    @Bean
    KnowledgeRetriever knowledgeRetriever(RuntimeConfig runtimeConfig, DifyRuntimeProperties properties) {
        if (runtimeConfig.knowledgeEndpoint().isPresent()) {
            return new HttpKnowledgeRetriever(runtimeConfig);
        }
        if (!properties.getBoundary().isAllowStubIntegrations() || properties.isFailOnMissingIntegrations()) {
            throw new IllegalStateException("Knowledge HTTP endpoint is required");
        }
        return new StubKnowledgeRetriever();
    }

    /**
     * 创建权限策略。
     * <p>
     * 根据安全边界配置中的最大查询长度和用户 ID 要求构建
     * {@link PermissionPolicy}，用于工作流执行前的请求校验。
     *
     * @param properties 全局运行时属性 / global runtime properties
     * @return 权限策略实例 / permission policy instance
     */
    @Bean
    PermissionPolicy permissionPolicy(DifyRuntimeProperties properties) {
        return new PermissionPolicy(
                properties.getBoundary().getMaxQueryLength(),
                properties.getBoundary().isRequireUserId());
    }

    /**
     * 创建输出净化器。
     * <p>
     * 根据安全边界配置中的最大输出长度构建 {@link OutputSanitizer}，
     * 用于工作流执行后对 Agent 输出的裁剪与过滤。
     *
     * @param properties 全局运行时属性 / global runtime properties
     * @return 输出净化器实例 / output sanitizer instance
     */
    @Bean
    OutputSanitizer outputSanitizer(DifyRuntimeProperties properties) {
        return new OutputSanitizer(properties.getBoundary().getMaxOutputLength());
    }

    /**
     * 创建 Nacos 资源目录。
     * <p>
     * 通过 {@link NacosResourceSynchronizer} 从 Nacos 配置中心同步
     * Prompt / Skill / MCP 等资源，并返回已加载的 {@link NacosResourceCatalog}。
     *
     * @param properties 全局运行时属性 / global runtime properties
     * @return Nacos 资源目录实例 / Nacos resource catalog instance
     */
    @Bean
    NacosResourceCatalog nacosResourceCatalog(DifyRuntimeProperties properties) {
        return new NacosResourceSynchronizer(properties).load();
    }

    /**
     * 创建执行追踪器。
     * <p>
     * 返回 {@link LoggingExecutionTracer} 实现，记录每次工作流执行的步骤与耗时。
     *
     * @return 执行追踪器实例 / execution tracer instance
     */
    @Bean
    ExecutionTracer executionTracer() {
        return new LoggingExecutionTracer();
    }

    /**
     * 创建短期会话记忆存储。
     *
     * @param properties 全局运行时配置
     * @return 启用时返回内存 session store，关闭时返回 NoOp 实现
     */
    @Bean
    SessionMemoryStore sessionMemoryStore(DifyRuntimeProperties properties) {
        if (!properties.getMemory().getSession().isEnabled()) {
            return new NoOpSessionMemoryStore();
        }
        return new InMemorySessionMemoryStore(properties.getMemory().getSession().getMaxTurns());
    }

    /**
     * 创建 AgentScope 官方 AgentStateStore。
     * <p>该 Bean 供所有运行期构建的 ReActAgent 共享。项目级 session memory 是否开启只影响
     * WorkflowService 的对话拼接；AgentScope 官方状态槽位始终保留，便于后续替换成持久化实现。</p>
     *
     * @return AgentScope 官方内存状态存储
     */
    @Bean
    AgentStateStore agentScopeAgentStateStore() {
        return new InMemoryAgentStateStore();
    }

    /**
     * 创建长期记忆存储。
     *
     * @param properties    全局运行时配置
     * @param runtimeConfig 通用运行时配置
     * @return 启用 Memos 时返回 HTTP 适配器，否则返回 NoOp 实现
     */
    @Bean
    MemoStore memoStore(DifyRuntimeProperties properties, RuntimeConfig runtimeConfig) {
        DifyRuntimeProperties.Memos memos = properties.getMemory().getMemos();
        if (memos.isEnabled()) {
            if (memos.getEndpoint() == null || blankToNull(memos.getAccessToken()) == null) {
                throw new IllegalStateException("Memos endpoint and access token are required when memos is enabled");
            }
            return new MemosMemoStore(memos, runtimeConfig);
        }
        return new NoOpMemoStore();
    }

    /**
     * 创建工作流注册表。
     * <p>
     * 遍历 {@link DifyRuntimeProperties#getWorkflows()} 中定义的所有工作流，
     * 依次完成 DSL 解析、生成代码写入、Agent 构建以及
     * {@link WorkflowExecutor} 装配，最终将所有 {@link WorkflowRuntimeBundle}
     * 注册到 {@link WorkflowRegistry} 中。
     *
     * @param properties         全局运行时属性 / global runtime properties
     * @param resourceLoader     Spring Resource 加载器 / resource loader
     * @param toolGateway        工具网关 / tool gateway
     * @param knowledgeRetriever 知识库检索器 / knowledge retriever
     * @param permissionPolicy   权限策略 / permission policy
     * @param outputSanitizer    输出净化器 / output sanitizer
     * @param executionTracer    执行追踪器 / execution tracer
     * @return 工作流注册表实例 / workflow registry instance
     */
    @Bean
    WorkflowRegistry workflowRegistry(
            DifyRuntimeProperties properties,
            ResourceLoader resourceLoader,
            ToolGateway toolGateway,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer executionTracer,
            MemoStore memoStore,
            AgentStateStore agentStateStore) {
        Map<String, WorkflowRuntimeBundle> bundles = new LinkedHashMap<>();
        WorkflowPlanWriter writer = new WorkflowPlanWriter();
        AgentScopeWorkflowPlanMapper agentScopeMapper = new AgentScopeWorkflowPlanMapper();
        properties.getWorkflows().forEach((workflowId, definition) -> {
            try {
                Resource resource = resourceLoader.getResource(definition.getLocation());
                WorkflowPlan plan = parseWorkflow(definition, resource);
                var agentScopePlan = agentScopeMapper.toAgentScope(plan);
                writer.write(plan, Path.of(properties.getGeneratedOutputDir()).resolve(workflowId));
                AgentInvoker invoker = agentInvoker(plan, toolGateway, properties, permissionPolicy, agentStateStore);
                WorkflowExecutor executor = new WorkflowExecutor(
                        workflowId,
                        plan,
                        invoker,
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
                bundles.put(workflowId, new WorkflowRuntimeBundle(workflowId, plan, agentScopePlan, executor));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load workflow " + workflowId, e);
            }
        });
        return new WorkflowRegistry(bundles);
    }

    private WorkflowPlan parseWorkflow(DifyRuntimeProperties.WorkflowDefinition definition, Resource resource)
            throws IOException {
        String sourceType = Optional.ofNullable(definition.getSourceType())
                .filter(value -> !value.isBlank())
                .orElse("dify")
                .toLowerCase();
        return switch (sourceType) {
            case "dify", "dify-dsl" -> new DifyDslParser().parse(resource.getInputStream());
            case "agentscope", "native", "workflow" -> new AgentScopeWorkflowParser().parse(resource.getInputStream());
            default -> throw new IllegalArgumentException("Unsupported workflow source type: " + sourceType);
        };
    }

    private AgentInvoker agentInvoker(
            WorkflowPlan plan,
            ToolGateway toolGateway,
            DifyRuntimeProperties properties,
            PermissionPolicy permissionPolicy,
            AgentStateStore agentStateStore) {
        if (!properties.isBuildAgents()) {
            return new StubAgentInvoker();
        }
        Map<String, ReActAgent> agents = new AgentScopeAgentFactory(
                permissionPolicy,
                agentStateStore).buildAgents(plan);
        return new AgentScopeStreamingAgentInvoker(agents, toolGateway);
    }

    /**
     * 将空白字符串转换为 {@code null}。
     * <p>
     * 若入参为 {@code null} 或仅包含空白字符则返回 {@code null}，
     * 否则原样返回。用于将配置文件中可能出现的空字符串统一转为
     * {@link Optional#empty()}，避免下游误认为有值。
     *
     * @param value 原始字符串 / raw string value
     * @return 转换后的值，空白时返回 {@code null} / {@code null} if blank, otherwise the original value
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
