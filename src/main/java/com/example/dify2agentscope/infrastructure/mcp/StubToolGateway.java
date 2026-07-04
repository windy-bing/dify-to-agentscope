package com.example.dify2agentscope.infrastructure.mcp;

import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.integration.ToolInvocation;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ToolGateway 的桩实现，返回模拟结果，用于开发或测试阶段。
 * <p>
 * Stub implementation of ToolGateway that returns mock results, used during development or testing.
 */
public class StubToolGateway implements ToolGateway {
    /**
     * 返回工具调用的模拟结果。
     *
     * @param invocation 工具调用请求
     * @return 标记为 stub 的工具响应
     */
    @Override
    public Map<String, Object> call(ToolInvocation invocation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", invocation.tool().name());
        result.put("provider", invocation.tool().providerName());
        result.put("status", "stub");
        result.put("message", "Tool gateway is not connected yet.");
        return result;
    }
}
