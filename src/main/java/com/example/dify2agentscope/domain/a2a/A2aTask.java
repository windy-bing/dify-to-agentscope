package com.example.dify2agentscope.domain.a2a;

import java.time.Instant;
import java.util.Map;

/**
 * A2A 任务实体。
 * <p>
 * 表示一个已提交的 A2A 任务，包含任务 ID、所属 Agent、当前状态、执行结果及时间信息。
 * A2A task entity — represents a submitted A2A task, including the task ID, agent ID,
 * current status, execution result, and timestamps.
 *
 * @param id        任务唯一标识 / unique task identifier
 * @param agentId   所属 Agent ID / agent ID
 * @param status    当前状态 / current status
 * @param result    执行结果 / execution result
 * @param error     错误信息 / error message
 * @param createdAt 创建时间 / creation timestamp
 * @param updatedAt 最后更新时间 / last update timestamp
 * @param metadata  附加元数据 / additional metadata
 */
public record A2aTask(
        String id,
        String agentId,
        A2aTaskStatus status,
        A2aTaskResult result,
        String error,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> metadata) {
}
