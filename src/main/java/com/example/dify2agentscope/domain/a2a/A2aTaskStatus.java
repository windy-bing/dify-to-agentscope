package com.example.dify2agentscope.domain.a2a;

/**
 * A2A 任务状态枚举。
 * <p>
 * 定义任务从提交到完成的整个生命周期状态。
 * Enumeration of A2A task statuses — defines the lifecycle states of a task from submission
 * to completion.
 */
public enum A2aTaskStatus {
    /** 已提交 / Task has been submitted */
    SUBMITTED,
    /** 执行中 / Task is running */
    RUNNING,
    /** 已完成 / Task completed successfully */
    COMPLETED,
    /** 已失败 / Task failed */
    FAILED
}
