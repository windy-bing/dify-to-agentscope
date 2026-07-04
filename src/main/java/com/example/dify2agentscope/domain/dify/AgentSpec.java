package com.example.dify2agentscope.domain.dify;

import java.util.List;

/**
 * Dify Agent 节点的规格说明。
 * <p>包含 Agent 的 system prompt（instruction）、模型标识、推理参数及绑定的工具列表。
 * 在 AgentScope 中对应一个 ReActAgent 的构建参数。
 */
public record AgentSpec(
        String nodeId,
        String title,
        String instruction,
        String model,
        Object completionParams,
        List<ToolSpec> tools) {
}
