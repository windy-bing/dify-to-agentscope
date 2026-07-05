package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.infrastructure.redis.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed AgentScope state store for AgentState values.
 */
public class RedisAgentStateStore implements AgentStateStore {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final RedisKeys keys;
    private final Duration ttl;

    public RedisAgentStateStore(StringRedisTemplate redis, String keyPrefix, Duration ttl) {
        this.redis = redis;
        this.keys = new RedisKeys(keyPrefix);
        this.ttl = ttl;
    }

    @Override
    public void save(String userId, String sessionId, String key, State state) {
        requireAgentState(state);
        String redisKey = keys.agentState(userId, sessionId, key);
        redis.opsForValue().set(redisKey, ((AgentState) state).toJson(), ttl);
        indexStateKey(userId, sessionId, redisKey);
        touchSession(userId, sessionId);
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> states) {
        List<String> payload = states.stream()
                .map(this::agentStateJson)
                .toList();
        try {
            String redisKey = keys.agentStateList(userId, sessionId, key);
            redis.opsForValue().set(redisKey, JSON.writeValueAsString(payload), ttl);
            indexStateKey(userId, sessionId, redisKey);
            touchSession(userId, sessionId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write AgentScope state list to Redis", e);
        }
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        String raw = redis.opsForValue().get(keys.agentState(userId, sessionId, key));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        State state = parseAgentState(raw);
        if (!type.isInstance(state)) {
            throw new ClassCastException("Stored state " + key + " is " + state.getClass().getName()
                    + ", not " + type.getName());
        }
        return Optional.of(type.cast(state));
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> type) {
        String raw = redis.opsForValue().get(keys.agentStateList(userId, sessionId, key));
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(raw, STRING_LIST).stream()
                    .map(this::parseAgentState)
                    .map(type::cast)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read AgentScope state list from Redis", e);
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(keys.agentSession(userId, sessionId)));
    }

    @Override
    public void delete(String userId, String sessionId) {
        String indexKey = keys.agentStateIndex(userId, sessionId);
        Set<String> stateKeys = redis.opsForSet().members(indexKey);
        if (stateKeys != null && !stateKeys.isEmpty()) {
            redis.delete(stateKeys);
        }
        redis.delete(indexKey);
        redis.delete(keys.agentSession(userId, sessionId));
        redis.opsForSet().remove(keys.agentSessionIndex(userId), sessionId);
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        String singleKey = keys.agentState(userId, sessionId, key);
        String listKey = keys.agentStateList(userId, sessionId, key);
        redis.delete(singleKey);
        redis.delete(listKey);
        redis.opsForSet().remove(keys.agentStateIndex(userId, sessionId), singleKey, listKey);
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        Set<String> sessionIds = redis.opsForSet().members(keys.agentSessionIndex(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(sessionIds);
    }

    private void touchSession(String userId, String sessionId) {
        redis.opsForValue().set(keys.agentSession(userId, sessionId), "1", ttl);
        String indexKey = keys.agentSessionIndex(userId);
        redis.opsForSet().add(indexKey, sessionId);
        redis.expire(indexKey, ttl);
    }

    private void indexStateKey(String userId, String sessionId, String redisKey) {
        String indexKey = keys.agentStateIndex(userId, sessionId);
        redis.opsForSet().add(indexKey, redisKey);
        redis.expire(indexKey, ttl);
    }

    private String agentStateJson(State state) {
        requireAgentState(state);
        return ((AgentState) state).toJson();
    }

    private void requireAgentState(State state) {
        if (!(state instanceof AgentState)) {
            throw new IllegalArgumentException("RedisAgentStateStore only supports AgentState, got "
                    + (state == null ? "null" : state.getClass().getName()));
        }
    }

    private AgentState parseAgentState(String raw) {
        return AgentState.fromJsonString(raw);
    }
}
