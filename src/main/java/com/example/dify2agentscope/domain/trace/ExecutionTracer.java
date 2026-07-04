package com.example.dify2agentscope.domain.trace;

import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import java.time.Duration;
import java.util.Map;

/**
 * 工作流执行链路日志接口。
 * <p>
 * 节点执行器只依赖该接口，具体落日志、上报 Studio 或接入 OpenTelemetry 都可以替换实现。
 * Workflow execution trace interface — node executors depend only on this interface,
 * allowing the underlying implementation to be swapped between logging, Studio reporting,
 * or OpenTelemetry integration.
 */
public interface ExecutionTracer {

    /**
     * 工作流开始执行时回调。
     * Called when a workflow starts execution.
     *
     * @param context 工作流执行上下文 / workflow execution context
     */
    void workflowStarted(WorkflowExecutionContext context);

    /**
     * 节点开始执行时回调。
     * Called when a node starts execution.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @param node    当前执行的节点 / the node being executed
     */
    void nodeStarted(WorkflowExecutionContext context, DifyNode node);

    /**
     * 节点执行成功时回调。
     * Called when a node finishes execution successfully.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param node     当前节点 / the node
     * @param handle   执行句柄（输出） / execution handle (output)
     * @param duration 执行耗时 / execution duration
     */
    void nodeFinished(WorkflowExecutionContext context, DifyNode node, String handle, Duration duration);

    /**
     * 节点执行失败时回调。
     * Called when a node fails execution.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param node     当前节点 / the node
     * @param duration 执行耗时 / execution duration
     * @param error    异常信息 / the exception
     */
    void nodeFailed(WorkflowExecutionContext context, DifyNode node, Duration duration, Exception error);

    /**
     * 工作流执行结束时回调。
     * Called when a workflow finishes execution.
     *
     * @param context  工作流执行上下文 / workflow execution context
     * @param success  是否成功 / whether the workflow succeeded
     * @param duration 总耗时 / total duration
     * @param metadata 附加元数据 / additional metadata
     */
    void workflowFinished(WorkflowExecutionContext context, boolean success, Duration duration, Map<String, Object> metadata);
}
