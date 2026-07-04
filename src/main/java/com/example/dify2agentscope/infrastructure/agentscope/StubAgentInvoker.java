package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;

/**
 * AgentInvoker 的桩实现，直接返回模拟结果，用于开发或测试阶段。
 * <p>
 * Stub implementation of AgentInvoker that returns a mock result directly, used during development or testing.
 */
public class StubAgentInvoker implements AgentInvoker {
    /**
     * 返回包含 Agent 标题和用户问题的模拟回复。
     *
     * @param agent   Agent 定义
     * @param context 工作流执行上下文
     * @return 模拟回复文本
     */
    @Override
    public String invoke(AgentSpec agent, WorkflowExecutionContext context) {
        return "[" + agent.title().trim() + " stub] " + context.request().query();
    }
}
