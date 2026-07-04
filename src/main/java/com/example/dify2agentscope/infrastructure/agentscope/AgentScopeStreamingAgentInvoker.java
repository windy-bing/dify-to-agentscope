package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.integration.ToolExecutionScope;
import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import java.util.Map;

/**
 * AgentInvoker 的 AgentScope 实现，通过流式事件驱动执行 Agent 并收集增量文本输出。
 * <p>
 * AgentScope implementation of AgentInvoker that executes agents via streaming events and collects incremental text output.
 */
public class AgentScopeStreamingAgentInvoker implements AgentInvoker {

    private final Map<String, ReActAgent> agents;
    private final ToolGateway toolGateway;

    /**
     * 创建流式 Agent 调用器。
     * <p>
     * Create streaming agent invoker.
     *
     * @param agents      节点 ID 到 ReActAgent 的映射 / mapping from node ID to ReActAgent
     * @param toolGateway 工具网关 / tool gateway for tool calls
     */
    public AgentScopeStreamingAgentInvoker(Map<String, ReActAgent> agents, ToolGateway toolGateway) {
        this.agents = agents;
        this.toolGateway = toolGateway;
    }

    /**
     * 调用指定 Agent 并返回完整响应文本。
     * <p>
     * Invoke the specified agent and return the full response text.
     *
     * @param agent   Agent 规格 / agent specification
     * @param context 工作流执行上下文 / workflow execution context
     * @return Agent 响应的完整文本 / full agent response text
     */
    @Override
    public String invoke(AgentSpec agent, WorkflowExecutionContext context) {
        ReActAgent reactAgent = agents.get(agent.nodeId());
        if (reactAgent == null) {
            throw new IllegalStateException("Agent not built: " + agent.nodeId());
        }
        StringBuilder answer = new StringBuilder();
        try (ToolExecutionScope ignored = ToolExecutionScope.open(context, toolGateway)) {
            reactAgent.streamEvents(new UserMessage(buildPrompt(context)), runtimeContext(context))
                    .doOnNext(event -> {
                        if (event instanceof TextBlockDeltaEvent delta) {
                            answer.append(delta.getDelta());
                        }
                    })
                    .blockLast();
        }
        return answer.toString();
    }

    /**
     * 将项目请求上下文映射为 AgentScope 官方 RuntimeContext。
     * <p>官方 ReActAgent 依赖 RuntimeContext 的 userId/sessionId 来做多用户、多会话隔离；
     * 本项目约定使用 userId + xDifyChatId 作为官方状态槽位，workflowId/traceId/menuId 作为本次调用扩展属性。</p>
     *
     * @param context 工作流执行上下文
     * @return AgentScope RuntimeContext
     */
    private RuntimeContext runtimeContext(WorkflowExecutionContext context) {
        return RuntimeContext.builder()
                .userId(blankToNull(context.request().userId()))
                .sessionId(sessionId(context))
                .put("workflowId", context.workflowId())
                .put("traceId", context.traceId())
                .put("xMenuId", context.request().xMenuId())
                .build();
    }

    /**
     * 生成传给 AgentScope 的官方 sessionId。
     *
     * @param context 工作流执行上下文
     * @return sessionId，优先使用 xDifyChatId，缺省时退回 traceId
     */
    private String sessionId(WorkflowExecutionContext context) {
        String chatId = blankToNull(context.request().xDifyChatId());
        return chatId == null ? context.traceId() : chatId;
    }

    /**
     * 将空白字符串规范化为 null，符合 RuntimeContext 对可选 userId 的语义。
     *
     * @param value 原始字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 根据执行上下文构建发送给 Agent 的提示文本。
     * <p>
     * Build prompt text sent to the agent from the execution context.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @return 提示文本 / prompt text
     */
    private String buildPrompt(WorkflowExecutionContext context) {
        return context.request().query()
                + "\n\n上下文:"
                + "\nconversation=" + context.conversation()
                + "\nnodeOutputs=" + context.nodeOutputs();
    }
}
