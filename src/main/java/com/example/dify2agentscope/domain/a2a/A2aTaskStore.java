package com.example.dify2agentscope.domain.a2a;

import java.util.Optional;

/**
 * A2A 任务存储接口。
 * <p>
 * 定义 A2A 任务的持久化操作，支持任务的保存和按 ID 查询。
 * A2A task store interface — defines persistence operations for A2A tasks, supporting saving
 * a task and looking up a task by its ID.
 */
public interface A2aTaskStore {

    /**
     * 保存（新增或更新）一个 A2A 任务。
     * Saves (inserts or updates) an A2A task.
     *
     * @param task 待保存的任务 / the task to save
     */
    void save(A2aTask task);

    /**
     * 根据任务 ID 查找任务。
     * Finds an A2A task by its ID.
     *
     * @param taskId 任务 ID / task identifier
     * @return 包含任务的可选值 / an Optional containing the task
     */
    Optional<A2aTask> find(String taskId);
}
