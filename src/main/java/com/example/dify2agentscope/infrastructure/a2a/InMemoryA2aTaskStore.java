package com.example.dify2agentscope.infrastructure.a2a;

import com.example.dify2agentscope.domain.a2a.A2aTask;
import com.example.dify2agentscope.domain.a2a.A2aTaskStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A2aTaskStore 的内存实现，使用 ConcurrentHashMap 存储任务。
 * <p>
 * In-memory implementation of A2aTaskStore using ConcurrentHashMap for task storage.
 * <p>该实现只适合本地开发或单实例部署。生产水平扩容时应提供 Redis/JDBC 等外部 {@link A2aTaskStore} Bean，
 * 生产水平扩容时应提供 Redis/JDBC 等外部 {@link A2aTaskStore} Bean。</p>
 */
public class InMemoryA2aTaskStore implements A2aTaskStore {

    private final ConcurrentMap<String, A2aTask> tasks = new ConcurrentHashMap<>();

    /**
     * 保存任务到内存存储中。
     * <p>
     * Save a task to the in-memory store.
     *
     * @param task 待保存的 A2A 任务 / A2A task to save
     */
    @Override
    public void save(A2aTask task) {
        tasks.put(task.id(), task);
    }

    /**
     * 根据任务 ID 查找任务。
     * <p>
     * Find a task by its ID.
     *
     * @param taskId 任务 ID / task ID
     * @return 包含任务的 Optional / Optional containing the task
     */
    @Override
    public Optional<A2aTask> find(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
