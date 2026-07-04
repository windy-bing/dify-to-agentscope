package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.domain.agentscope.AgentScopeWorkflowPlan;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;

/**
 * 单个 DSL 导入后的运行时对象。
 * <p>迁移期同时保留 legacy plan 和 AgentScope 规范 plan：legacy plan 供现有 DAG 执行器兼容，
 * agentScopePlan 作为后续官方优先改造的目标模型。</p>
 */
public record WorkflowRuntimeBundle(
        String workflowId,
        WorkflowPlan plan,
        AgentScopeWorkflowPlan agentScopePlan,
        WorkflowExecutor executor) {

    /**
     * 兼容旧构造方式。
     *
     * @param workflowId 工作流 ID
     * @param plan       legacy 计划
     * @param executor   执行器
     */
    public WorkflowRuntimeBundle(String workflowId, WorkflowPlan plan, WorkflowExecutor executor) {
        this(workflowId, plan, null, executor);
    }
}
