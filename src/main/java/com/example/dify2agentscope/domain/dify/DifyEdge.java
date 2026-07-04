package com.example.dify2agentscope.domain.dify;

/**
 * Dify 工作流中的有向边。
 * <p>{@code source}/{@code target} 为节点 ID；{@code sourceHandle} 标识条件分支的输出端（如 "true"/"false" 或 case_id），
 * 用于 if-else 节点的多路路由。
 */
public record DifyEdge(
        String id,
        String source,
        String sourceHandle,
        String target) {
}
