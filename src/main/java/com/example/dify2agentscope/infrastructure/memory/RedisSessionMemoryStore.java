package com.example.dify2agentscope.infrastructure.memory;

import com.example.dify2agentscope.domain.memory.SessionMemoryStore;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.infrastructure.redis.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed short-term session memory.
 */
public class RedisSessionMemoryStore implements SessionMemoryStore {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final RedisKeys keys;
    private final Duration ttl;
    private final int maxTurns;

    public RedisSessionMemoryStore(StringRedisTemplate redis, String keyPrefix, Duration ttl, int maxTurns) {
        this.redis = redis;
        this.keys = new RedisKeys(keyPrefix);
        this.ttl = ttl;
        this.maxTurns = maxTurns;
    }

    @Override
    public Map<String, Object> load(String workflowId, WorkflowRequest request) {
        String raw = redis.opsForValue().get(key(workflowId, request));
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.readValue(raw, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read session memory from Redis", e);
        }
    }

    @Override
    public void save(String workflowId, WorkflowRequest request, WorkflowResponse response) {
        if (!response.success()) {
            return;
        }
        try {
            Map<String, Object> state = new LinkedHashMap<>(response.conversation());
            List<Map<String, Object>> turns = previousTurns(state.get("_session_turns"));
            turns.add(Map.of("query", request.query(), "answer", response.answer(), "traceId", response.traceId()));
            while (turns.size() > maxTurns) {
                turns.remove(0);
            }
            state.put("_session_turns", turns);
            state.put("history_user_query", turns.stream().map(turn -> String.valueOf(turn.get("query"))).toList());
            state.put("reply", response.answer());
            redis.opsForValue().set(key(workflowId, request), JSON.writeValueAsString(state), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write session memory to Redis", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> previousTurns(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> turns = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    turns.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
            return turns;
        }
        return new ArrayList<>();
    }

    private String key(String workflowId, WorkflowRequest request) {
        return keys.sessionMemory(workflowId, request.userId(), request.xDifyChatId());
    }
}
