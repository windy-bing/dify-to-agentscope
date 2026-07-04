package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.domain.dify.WorkflowPlan;

/**
 * Java code-first workflow 定义结果。
 * <p>业务代码通过 {@link CodeFirstWorkflowBuilder} 一步步声明 workflow 后，得到该对象并交给
 * {@link CodeFirstWorkflowRuntime} 注册到现有运行时。</p>
 *
 * @param workflowId workflow 注册 ID
 * @param plan       兼容工作流计划
 */
public record CodeFirstWorkflowDefinition(
        String workflowId,
        WorkflowPlan plan) {
}
