package com.example.dify2agentscope.infrastructure.a2a;

import com.example.dify2agentscope.domain.a2a.A2aTask;
import com.example.dify2agentscope.domain.a2a.A2aTaskStore;
import com.example.dify2agentscope.infrastructure.redis.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed A2A task store.
 */
public class RedisA2aTaskStore implements A2aTaskStore {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    private final StringRedisTemplate redis;
    private final RedisKeys keys;
    private final Duration ttl;

    public RedisA2aTaskStore(StringRedisTemplate redis, String keyPrefix, Duration ttl) {
        this.redis = redis;
        this.keys = new RedisKeys(keyPrefix);
        this.ttl = ttl;
    }

    @Override
    public void save(A2aTask task) {
        try {
            redis.opsForValue().set(keys.a2aTask(task.id()), JSON.writeValueAsString(task), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write A2A task to Redis", e);
        }
    }

    @Override
    public Optional<A2aTask> find(String taskId) {
        String raw = redis.opsForValue().get(keys.a2aTask(taskId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readValue(raw, A2aTask.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read A2A task from Redis", e);
        }
    }
}
