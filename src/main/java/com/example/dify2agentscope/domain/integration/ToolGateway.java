package com.example.dify2agentscope.domain.integration;

import java.util.Map;

/**
 * MCP/工具调用的出站边界。<br>
 * Outbound boundary for MCP/tool calls.
 * 实现类只负责传输协议，权限校验必须在进入该边界前由 PermissionPolicy 完成。
 * Implementation classes are only responsible for the transport protocol;
 * permission checks must be completed by PermissionPolicy before entering this boundary.
 */
public interface ToolGateway {

    /**
     * 执行工具调用并返回结果。<br>
     * Execute a tool call and return the result.
     *
     * @param invocation 工具调用信息 / tool invocation information
     * @return 调用结果键值对 / key-value pairs of the call result
     */
    Map<String, Object> call(ToolInvocation invocation);
}
