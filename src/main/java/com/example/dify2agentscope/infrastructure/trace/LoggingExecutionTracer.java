package com.example.dify2agentscope.infrastructure.trace;

import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.trace.ExecutionTracer;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ExecutionTracer 的日志实现，通过 SLF4J 输出执行事件，并利用 MDC 传递 traceId / workflowId。
 * <p>
 * Logging implementation of ExecutionTracer that outputs execution events via SLF4J, using MDC to propagate traceId / workflowId.
 */
public class LoggingExecutionTracer implements ExecutionTracer {

    private static final Logger log = LoggerFactory.getLogger(LoggingExecutionTracer.class);

    /**
     * 记录工作流开始事件。
     * <p>
     * Log workflow started event.
     *
     * @param context 工作流执行上下文 / workflow execution context
     */
    @Override
    public void workflowStarted(WorkflowExecutionContext context) {
        withTrace(context, () -> log.info(
                "\u5de5\u4f5c\u6d41\u5f00\u59cb workflowId={} traceId={} userId={} chatId={}",
                context.workflowId(),
                context.traceId(),
                context.request().userId(),
                context.request().xDifyChatId()));
    }

    /**
     * 记录节点开始执行事件。
     * <p>
     * Log node started event.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @param node    当前节点 / current node
     */
    @Override
    public void nodeStarted(WorkflowExecutionContext context, DifyNode node) {
        withTrace(context, () -> log.info(
                "\u8282\u70b9\u5f00\u59cb workflowId={} traceId={} nodeId={} nodeType={} nodeTitle={}",
                context.workflowId(),
                context.traceId(),
                node.id(),
                node.type(),
                node.title()));
    }

    /**
     * 记录节点完成事件。
     * <p>
     * Log node finished event.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param node     已完成节点 / finished node
     * @param handle   输出 handle 标识 / output handle identifier
     * @param duration 执行耗时 / execution duration
     */
    @Override
    public void nodeFinished(
            WorkflowExecutionContext context,
            DifyNode node,
            String handle,
            Duration duration) {
        withTrace(context, () -> log.info(
                "\u8282\u70b9\u5b8c\u6210 workflowId={} traceId={} nodeId={} nodeType={} handle={} durationMs={}",
                context.workflowId(),
                context.traceId(),
                node.id(),
                node.type(),
                handle,
                duration.toMillis()));
    }

    /**
     * 记录节点失败事件。
     * <p>
     * Log node failed event.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param node     失败节点 / failed node
     * @param duration 执行耗时 / execution duration
     * @param error    异常 / exception
     */
    @Override
    public void nodeFailed(
            WorkflowExecutionContext context,
            DifyNode node,
            Duration duration,
            Exception error) {
        withTrace(context, () -> log.warn(
                "\u8282\u70b9\u5931\u8d25 workflowId={} traceId={} nodeId={} nodeType={} durationMs={} error={}",
                context.workflowId(),
                context.traceId(),
                node.id(),
                node.type(),
                duration.toMillis(),
                error.getMessage(),
                error));
    }

    /**
     * 记录工作流结束事件。
     * <p>
     * Log workflow finished event.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param success  是否成功 / whether successful
     * @param duration 执行耗时 / execution duration
     * @param metadata 附加元数据 / additional metadata
     */
    @Override
    public void workflowFinished(
            WorkflowExecutionContext context,
            boolean success,
            Duration duration,
            Map<String, Object> metadata) {
        withTrace(context, () -> log.info(
                "\u5de5\u4f5c\u6d41\u7ed3\u675f workflowId={} traceId={} success={} durationMs={} path={}",
                context.workflowId(),
                context.traceId(),
                success,
                duration.toMillis(),
                context.executionPath()));
    }

    /**
     * 在 MDC 中设置 traceId 和 workflowId，执行日志记录后恢复原始值。
     * <p>
     * Set traceId and workflowId in MDC, execute logging, then restore original values.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param runnable 待执行的日志操作 / logging operation to execute
     */
    private void withTrace(WorkflowExecutionContext context, Runnable runnable) {
        String previousTraceId = MDC.get("traceId");
        String previousWorkflowId = MDC.get("workflowId");
        try {
            MDC.put("traceId", context.traceId());
            MDC.put("workflowId", context.workflowId());
            runnable.run();
        } finally {
            restore("traceId", previousTraceId);
            restore("workflowId", previousWorkflowId);
        }
    }

    /**
     * 恢复 MDC 中的某个键的原始值，如果原始值不存在则移除该键。
     * <p>
     * Restore a key in MDC to its original value, or remove the key if original value is null.
     *
     * @param key   MDC 键 / MDC key
     * @param value 原始值 / original value
     */
    private void restore(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
