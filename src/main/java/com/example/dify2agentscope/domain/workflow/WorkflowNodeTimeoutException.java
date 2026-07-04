package com.example.dify2agentscope.domain.workflow;

import java.time.Duration;

/**
 * 节点执行超时时抛出的业务异常。
 * <p>用于把“节点超过配置处理时长”与普通业务异常区分开，便于 API 响应、日志和节点输出记录统一识别。
 */
public class WorkflowNodeTimeoutException extends RuntimeException {

    private final String nodeId;
    private final Duration timeout;

    /**
     * 创建节点超时异常。
     *
     * @param nodeId  超时节点 ID
     * @param timeout 节点允许的最大执行时长
     */
    public WorkflowNodeTimeoutException(String nodeId, Duration timeout) {
        super("Node " + nodeId + " timed out after " + timeout.toMillis() + "ms");
        this.nodeId = nodeId;
        this.timeout = timeout;
    }

    /**
     * 获取超时节点 ID。
     *
     * @return 节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 获取节点超时配置。
     *
     * @return 超时时长
     */
    public Duration getTimeout() {
        return timeout;
    }
}
