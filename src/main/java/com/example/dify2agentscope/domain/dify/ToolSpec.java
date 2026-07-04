package com.example.dify2agentscope.domain.dify;

/**
 * Dify Agent 节点上绑定的 MCP 工具声明。
 * <p>包含工具标识、提供方、描述及参数定义。
 * 运行时通过 ToolGateway 将工具调用转发到 Higress MCP 或其他实现。
 */
public record ToolSpec(
        String name,
        String label,
        String description,
        String providerName,
        Object parameters) {
}
