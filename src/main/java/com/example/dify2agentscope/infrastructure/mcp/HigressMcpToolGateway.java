package com.example.dify2agentscope.infrastructure.mcp;

import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.domain.integration.ToolInvocation;
import com.example.dify2agentscope.domain.workflow.RuntimeConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Higress MCP 协议的 ToolGateway 实现，通过 JSON-RPC over HTTP 调用工具。
 * <p>
 * ToolGateway implementation based on the Higress MCP protocol, invoking tools via JSON-RPC over HTTP.
 */
public class HigressMcpToolGateway implements ToolGateway {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicLong IDS = new AtomicLong(1);

    private final RuntimeConfig config;
    private final HttpClient httpClient;

    /**
     * 创建 Higress MCP 工具网关。
     * <p>
     * Create Higress MCP tool gateway.
     *
     * @param config 运行时配置 / runtime configuration
     */
    public HigressMcpToolGateway(RuntimeConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .build();
    }

    /**
     * 调用远程 MCP 工具并返回结果。
     * <p>
     * Invoke a remote MCP tool and return the result.
     *
     * @param invocation 工具调用信息 / tool invocation details
     * @return 工具返回的键值对 / key-value result returned by the tool
     */
    @Override
    public Map<String, Object> call(ToolInvocation invocation) {
        URI endpoint = config.higressMcpEndpoint()
                .orElseThrow(() -> new IllegalStateException("HIGRESS_MCP_ENDPOINT is not configured"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", IDS.getAndIncrement());
        payload.put("method", "tools/call");
        payload.put("params", Map.of(
                "name", invocation.tool().name(),
                "arguments", invocation.input()));
        return postJson(endpoint, payload);
    }

    /**
     * 发送 HTTP POST 请求并解析响应。
     * <p>
     * Send an HTTP POST request and parse the response.
     *
     * @param endpoint 请求地址 / request endpoint
     * @param payload  请求体 JSON 数据 / request body JSON data
     * @return 解析后的响应映射 / parsed response map
     */
    private Map<String, Object> postJson(URI endpoint, Map<String, Object> payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(config.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload)));
            config.higressMcpBearerToken().ifPresent(token -> builder.header("Authorization", "Bearer " + token));
            config.extraHeaders().forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Higress MCP HTTP " + response.statusCode() + ": " + response.body());
            }
            return parseBody(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Higress MCP", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Higress MCP", e);
        }
    }

    /**
     * 解析 HTTP 响应体，支持 SSE 格式（data: 前缀）或纯 JSON 格式。
     * <p>
     * Parse HTTP response body, supporting SSE format (data: prefix) or plain JSON.
     *
     * @param body HTTP 响应体 / HTTP response body
     * @return 解析后的 JSON Map / parsed JSON map
     * @throws IOException JSON 解析失败 / if JSON parsing fails
     */
    private Map<String, Object> parseBody(String body) throws IOException {
        String json = body == null ? "" : body.trim();
        if (json.startsWith("data:")) {
            json = json.lines()
                    .filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring(5).trim())
                    .filter(line -> !line.equals("[DONE]"))
                    .findFirst()
                    .orElse("{}");
        }
        return JSON.readValue(json, new TypeReference<>() {});
    }
}
