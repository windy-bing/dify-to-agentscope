package com.example.dify2agentscope.infrastructure.redis;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Redis key helper that keeps all persisted runtime state under one prefix.
 */
public final class RedisKeys {

    private final String prefix;

    public RedisKeys(String prefix) {
        this.prefix = blank(prefix) ? "dify-to-agentscope" : prefix;
    }

    public String sessionMemory(String workflowId, String userId, String chatId) {
        return key("session", workflowId, userId, chatId);
    }

    public String a2aTask(String taskId) {
        return key("a2a", "task", taskId);
    }

    public String agentSession(String userId, String sessionId) {
        return key("agent-session", normalizeUser(userId), sessionId);
    }

    public String agentSessionIndex(String userId) {
        return key("agent-session-index", normalizeUser(userId));
    }

    public String agentStateIndex(String userId, String sessionId) {
        return key("agent-state-index", normalizeUser(userId), sessionId);
    }

    public String agentState(String userId, String sessionId, String key) {
        return key("agent-state", normalizeUser(userId), sessionId, "single", key);
    }

    public String agentStateList(String userId, String sessionId, String key) {
        return key("agent-state", normalizeUser(userId), sessionId, "list", key);
    }

    private String key(String first, String... parts) {
        StringBuilder builder = new StringBuilder(prefix).append(':').append(first);
        for (String part : parts) {
            builder.append(':').append(encode(part));
        }
        return builder.toString();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeUser(String userId) {
        return blank(userId) ? "__anon__" : userId;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
