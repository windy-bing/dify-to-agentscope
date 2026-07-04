package com.example.dify2agentscope.domain.integration;

import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import java.util.Map;

/**
 * 工具调用记录，封装一次工具调用的全部参数。<br>
 * Tool invocation record that encapsulates all parameters of a tool call.
 *
 * @param agent   发起调用的 Agent 规格 / the agent specification making the call
 * @param tool    目标工具规格 / the target tool specification
 * @param input   调用输入参数 / input parameters for the call
 * @param context 工作流执行上下文 / workflow execution context
 */
public record ToolInvocation(
        AgentSpec agent,
        ToolSpec tool,
        Map<String, Object> input,
        WorkflowExecutionContext context) {
}
