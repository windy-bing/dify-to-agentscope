package com.example.dify2agentscope.domain.integration;

import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;

/**
 * 工具执行作用域，基于 ThreadLocal 绑定当前工作流上下文和工具网关。<br>
 * Tool execution scope that binds the current workflow context and tool gateway
 * via ThreadLocal.
 * 实现了 {@link AutoCloseable}，可在 try-with-resources 中使用。
 * Implements {@link AutoCloseable} for use with try-with-resources.
 */
public final class ToolExecutionScope implements AutoCloseable {

    private static final ThreadLocal<WorkflowExecutionContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<ToolGateway> GATEWAY = new ThreadLocal<>();

    /**
     * 私有构造器，将上下文和网关绑定到当前线程。<br>
     * Private constructor; binds the context and gateway to the current thread.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @param gateway 工具网关实例 / tool gateway instance
     */
    private ToolExecutionScope(WorkflowExecutionContext context, ToolGateway gateway) {
        CONTEXT.set(context);
        GATEWAY.set(gateway);
    }

    /**
     * 打开一个新的执行作用域并绑定到当前线程。<br>
     * Open a new execution scope and bind it to the current thread.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @param gateway 工具网关实例 / tool gateway instance
     * @return 新的 ToolExecutionScope 实例 / a new ToolExecutionScope instance
     */
    public static ToolExecutionScope open(WorkflowExecutionContext context, ToolGateway gateway) {
        return new ToolExecutionScope(context, gateway);
    }

    /**
     * 获取当前线程绑定的工作流执行上下文。<br>
     * Get the workflow execution context bound to the current thread.
     *
     * @return 工作流执行上下文 / workflow execution context
     */
    public static WorkflowExecutionContext context() {
        return CONTEXT.get();
    }

    /**
     * 获取当前线程绑定的工具网关。<br>
     * Get the tool gateway bound to the current thread.
     *
     * @return 工具网关实例 / tool gateway instance
     * @throws IllegalStateException 如果网关未绑定 / if the gateway is not bound
     */
    public static ToolGateway gateway() {
        ToolGateway gateway = GATEWAY.get();
        if (gateway == null) {
            throw new IllegalStateException("Tool gateway is not bound");
        }
        return gateway;
    }

    /**
     * 关闭作用域，清理当前线程的绑定资源。<br>
     * Close the scope and clean up the thread-local bindings.
     */
    @Override
    public void close() {
        CONTEXT.remove();
        GATEWAY.remove();
    }
}
