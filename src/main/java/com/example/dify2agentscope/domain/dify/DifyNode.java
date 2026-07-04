package com.example.dify2agentscope.domain.dify;

/**
 * Dify 工作流中的单个图节点。
 * <p>{@code type} 决定节点语义：start / code / if-else / assigner / knowledge-retrieval / agent / answer 等。
 * {@code data} 为节点配置的原始 Map，由具体执行器按 type 解析。
 */
public record DifyNode(
        String id,
        String type,
        String title,
        Object data) {
}
