package com.example.dify2agentscope.domain.agentscope;

/**
 * AgentScope DAG 兼容模式下的边。
 * <p>只在 {@link AgentScopeExecutionMode#DAG} 模式下作为编排边使用；自循环模式下应尽量依赖
 * AgentScope 官方 ReAct loop / Harness loop，而不是手写外部边。</p>
 *
 * @param id     边 ID
 * @param source 源节点 ID
 * @param handle 源输出句柄
 * @param target 目标节点 ID
 */
public record AgentScopeEdge(
        String id,
        String source,
        String handle,
        String target) {
}
