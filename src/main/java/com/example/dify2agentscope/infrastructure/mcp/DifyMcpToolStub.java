package com.example.dify2agentscope.infrastructure.mcp;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.integration.ToolExecutionScope;
import com.example.dify2agentscope.domain.integration.ToolInvocation;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import com.example.dify2agentscope.infrastructure.agentscope.DifyValueResolver;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 将 Dify ToolSpec 适配为 AgentScope AgentTool，在 AgentScope 智能体中嵌入 Dify 工具调用能力。
 * <p>
 * Adapter that wraps Dify ToolSpec as an AgentScope AgentTool, embedding Dify tool invocation capability in AgentScope agents.
 */
public class DifyMcpToolStub implements AgentTool {

    private final AgentSpec agent;
    private final ToolSpec spec;
    private final PermissionPolicy permissionPolicy;
    private final DifyValueResolver valueResolver = new DifyValueResolver();

    /**
     * 创建 Dify 工具桩适配器。
     * <p>
     * Create Dify tool stub adapter.
     *
     * @param agent            所属 Agent 规格 / owning agent specification
     * @param spec             工具规格 / tool specification
     * @param permissionPolicy 权限策略 / permission policy
     */
    public DifyMcpToolStub(AgentSpec agent, ToolSpec spec, PermissionPolicy permissionPolicy) {
        this.agent = agent;
        this.spec = spec;
        this.permissionPolicy = permissionPolicy;
    }

    /**
     * 获取工具名称（将 . 和 - 替换为 _）。
     * <p>
     * Get tool name (replacing . and - with _).
     *
     * @return 工具名称 / tool name
     */
    @Override
    public String getName() {
        return spec.name().replace('.', '_').replace('-', '_');
    }

    /**
     * 获取工具描述。
     * <p>
     * Get tool description.
     *
     * @return 工具描述 / tool description
     */
    @Override
    public String getDescription() {
        return spec.description();
    }

    /**
     * 获取工具参数的 JSON Schema。
     * <p>
     * Get the JSON Schema for tool parameters.
     *
     * @return 参数 Schema / parameter schema
     */
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", spec.description());
        Map<String, Object> properties = new LinkedHashMap<>();
        if (spec.parameters() instanceof Map<?, ?> params) {
            for (Map.Entry<?, ?> entry : params.entrySet()) {
                String name = String.valueOf(entry.getKey());
                properties.put(name, Map.of(
                        "type", "string",
                        "description", "Dify tool param: " + name));
            }
        }
        if (!properties.isEmpty()) {
            schema.put("properties", properties);
        }
        return schema;
    }

    /**
     * 异步调用工具，在 ToolExecutionScope 上下文中执行权限校验、参数解析和网关转发。
     * <p>
     * Call the tool asynchronously, performing permission validation, parameter resolution and gateway forwarding in ToolExecutionScope context.
     *
     * @param param 工具调用参数 / tool call parameters
     * @return 包含调用结果的 Mono / Mono containing the call result
     */
    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        WorkflowExecutionContext context = ToolExecutionScope.context();
        if (context == null) {
            throw new IllegalStateException("\u5de5\u5177\u6267\u884c\u4e0a\u4e0b\u6587\u672a\u7ed1\u5b9a");
        }
        permissionPolicy.validateToolCall(context, agent, spec);
        Map<String, Object> input = new LinkedHashMap<>(valueResolver.resolveToolParameters(spec.parameters(), context));
        input.putAll(param.getInput());
        Map<String, Object> gatewayResult = ToolExecutionScope.gateway()
                .call(new ToolInvocation(agent, spec, input, context));
        String message = String.valueOf(gatewayResult);
        ToolResultBlock result = new ToolResultBlock(
                param.getToolUseBlock().getId(),
                getName(),
                List.of(TextBlock.builder().text(message).build()),
                Map.of(),
                ToolResultState.SUCCESS);
        return Mono.just(result);
    }
}
