package com.example.dify2agentscope.domain.security;

/**
 * 工作流安全异常。
 * <p>
 * 当请求校验或工具权限校验不通过时抛出，触发该异常后将终止工作流执行并返回错误信息。
 * Workflow security exception — thrown when request validation or tool permission validation
 * fails, causing the workflow execution to be aborted with an error response.
 */
public class WorkflowSecurityException extends RuntimeException {

    /**
     * 使用指定的错误消息构造异常。
     * Constructs an exception with the specified detail message.
     *
     * @param message 错误描述 / error description
     */
    public WorkflowSecurityException(String message) {
        super(message);
    }
}
