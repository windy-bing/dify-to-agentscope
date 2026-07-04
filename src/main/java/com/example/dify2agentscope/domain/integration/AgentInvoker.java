package com.example.dify2agentscope.domain.integration;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;

/**
 * Agent 节点调用边界。<br>
 * Agent node invocation boundary.
 * 应用层只依赖该接口，因此 AgentScope、测试桩或其他 Agent 运行时可以独立替换。
 * The application layer depends only on this interface, allowing AgentScope,
 * test stubs, or other Agent runtimes to be replaced independently.
 */
public interface AgentInvoker {

    /**
     * 执行 Agent 节点并返回结果字符串。<br>
     * Execute the agent node and return the result string.
     *
     * @param agent    Agent 规格定义 / agent specification definition
     * @param context  工作流执行上下文 / workflow execution context
     * @return Agent 执行结果字符串 / agent execution result string
     */
    String invoke(AgentSpec agent, WorkflowExecutionContext context);
}
