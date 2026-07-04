package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.agentscope.AgentScopeCapabilityManifest;
import com.example.dify2agentscope.domain.agentscope.AgentScopeOfficialCapability;
import com.example.dify2agentscope.infrastructure.mcp.DifyMcpToolStub;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 WorkflowPlan 中的 Agent 定义构建为 AgentScope ReActAgent 实例。
 * <p>
 * 这里使用的是 AgentScope Java 官方的 {@link ReActAgent} 和 {@link Toolkit}；WorkflowPlan、Dify 节点、
 * MCP 网关转发是本项目为了迁移和编排补充的适配层，不属于 AgentScope Java 官方核心 API。
 */
public class AgentScopeAgentFactory {

    private final PermissionPolicy permissionPolicy;
    private final AgentStateStore agentStateStore;

    /**
     * 使用默认权限策略创建工厂。
     * <p>
     * Create factory with default permission policy.
     */
    public AgentScopeAgentFactory() {
        this(new PermissionPolicy(), null);
    }

    /**
     * 使用指定的权限策略创建工厂。
     * <p>
     * Create factory with the specified permission policy.
     *
     * @param permissionPolicy 权限策略 / permission policy
     */
    public AgentScopeAgentFactory(PermissionPolicy permissionPolicy) {
        this(permissionPolicy, null);
    }

    /**
     * 使用指定权限策略和官方 AgentStateStore 创建工厂。
     * <p>当 stateStore 不为空时，ReActAgent 会按官方 RuntimeContext 中的 userId/sessionId
     * 自动加载和保存 AgentState，这是多用户多会话隔离的官方路径。</p>
     *
     * @param permissionPolicy 权限策略
     * @param agentStateStore  AgentScope 官方状态存储，null 表示只使用 Agent 内存态
     */
    public AgentScopeAgentFactory(PermissionPolicy permissionPolicy, AgentStateStore agentStateStore) {
        this.permissionPolicy = permissionPolicy;
        this.agentStateStore = agentStateStore;
    }

    /**
     * 为工作流计划中所有 Agent 构建 ReActAgent 实例。
     * <p>
     * Build ReActAgent instances for all agents in the workflow plan.
     *
     * @param plan 工作流计划 / workflow plan
     * @return 节点 ID 到 ReActAgent 的映射 / mapping from node ID to ReActAgent
     */
    public Map<String, ReActAgent> buildAgents(WorkflowPlan plan) {
        Map<String, ReActAgent> agents = new LinkedHashMap<>();
        for (AgentSpec spec : plan.agents()) {
            agents.put(spec.nodeId(), buildAgent(spec));
        }
        return agents;
    }

    /**
     * 暴露当前项目保留的 AgentScope 官方能力清单。
     * <p>该方法不参与构建逻辑，主要给文档、诊断接口和后续扩展点复用。</p>
     *
     * @return AgentScope 官方能力条目列表
     */
    public List<AgentScopeOfficialCapability> officialCapabilities() {
        return AgentScopeCapabilityManifest.all();
    }

    /**
     * 根据 Agent 规格构建单个 ReActAgent，包括注册工具。
     * <p>
     * Build a single ReActAgent from AgentSpec, including tool registration.
     *
     * @param spec Agent 规格 / agent specification
     * @return 构建的 ReActAgent / built ReActAgent
     */
    public ReActAgent buildAgent(AgentSpec spec) {
        Toolkit toolkit = new Toolkit();
        spec.tools().forEach(tool -> toolkit.registerTool(new DifyMcpToolStub(spec, tool, permissionPolicy)));

        ReActAgent.Builder builder = ReActAgent.builder()
                .name(spec.title())
                .sysPrompt(spec.instruction())
                .model(toAgentScopeModel(spec.model()))
                .toolkit(toolkit)
                .defaultSessionId(spec.nodeId());
        if (agentStateStore != null) {
            builder.stateStore(agentStateStore);
        }
        return builder.build();
    }

    /**
     * 将 Dify 模型标识符转换为 AgentScope 格式（modelType:modelName）。
     * <p>
     * Convert Dify model identifier to AgentScope format (modelType:modelName).
     *
     * @param difyModel Dify 模型字符串 / Dify model string
     * @return AgentScope 格式的模型标识 / model identifier in AgentScope format
     */
    private String toAgentScopeModel(String difyModel) {
        if (difyModel == null || difyModel.isBlank()) {
            return "dashscope:qwen-plus";
        }
        if (difyModel.contains(":")) {
            return difyModel;
        }
        if (difyModel.startsWith("qwen")) {
            return "dashscope:" + difyModel;
        }
        return difyModel;
    }
}
