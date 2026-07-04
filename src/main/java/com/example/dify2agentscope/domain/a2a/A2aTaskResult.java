package com.example.dify2agentscope.domain.a2a;

import java.util.Map;

/**
 * A2A 任务执行结果。
 * <p>
 * 包含 Agent 的回复文本、会话上下文、产出的工件以及附加元数据。
 * A2A task result — contains the agent's answer text, conversation context, produced artifacts,
 * and additional metadata.
 *
 * @param answer       Agent 回复文本 / agent answer text
 * @param conversation 会话上下文 / conversation context
 * @param artifacts    产出工件 / produced artifacts
 * @param metadata     附加元数据 / additional metadata
 */
public record A2aTaskResult(
        String answer,
        Map<String, Object> conversation,
        Map<String, Object> artifacts,
        Map<String, Object> metadata) {
}
