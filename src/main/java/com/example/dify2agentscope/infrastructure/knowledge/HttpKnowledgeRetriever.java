package com.example.dify2agentscope.infrastructure.knowledge;

import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.workflow.RuntimeConfig;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 HTTP 端点调用的知识检索实现，支持 Bearer Token 认证。
 * <p>
 * Knowledge retrieval implementation based on HTTP endpoint invocation with Bearer Token authentication.
 */
public class HttpKnowledgeRetriever implements KnowledgeRetriever {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RuntimeConfig config;
    private final HttpClient httpClient;

    /**
     * 创建 HTTP 知识检索器。
     * <p>
     * Create HTTP knowledge retriever.
     *
     * @param config 运行时配置 / runtime configuration
     */
    public HttpKnowledgeRetriever(RuntimeConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .build();
    }

    /**
     * 根据 Dify 知识库节点信息检索相关知识。
     * <p>
     * Retrieve relevant knowledge based on Dify knowledge node information.
     *
     * @param node    Dify 知识库节点 / Dify knowledge node
     * @param context 工作流执行上下文 / workflow execution context
     * @return 检索结果键值对 / retrieval result key-value pairs
     */
    @Override
    public Map<String, Object> retrieve(DifyNode node, WorkflowExecutionContext context) {
        URI endpoint = config.knowledgeEndpoint()
                .orElseThrow(() -> new IllegalStateException("KNOWLEDGE_HTTP_ENDPOINT is not configured"));
        Map<String, Object> nodeData = castMap(node.data());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", context.request().query());
        payload.put("dataset_ids", nodeData.get("dataset_ids"));
        payload.put("retrieval_mode", nodeData.get("retrieval_mode"));
        payload.put("multiple_retrieval_config", nodeData.get("multiple_retrieval_config"));
        return postJson(endpoint, payload);
    }

    /**
     * 发送 HTTP POST 请求并解析返回的 JSON 结果。
     * <p>
     * Send an HTTP POST request and parse the returned JSON result.
     *
     * @param endpoint 请求地址 / request endpoint
     * @param payload  请求体 / request payload
     * @return 解析后的响应映射 / parsed response map
     */
    private Map<String, Object> postJson(URI endpoint, Map<String, Object> payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(config.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload)));
            config.knowledgeBearerToken().ifPresent(token -> builder.header("Authorization", "Bearer " + token));
            config.extraHeaders().forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Knowledge HTTP " + response.statusCode() + ": " + response.body());
            }
            Map<String, Object> parsed = JSON.readValue(response.body(), new TypeReference<>() {});
            if (!parsed.containsKey("result")) {
                parsed.put("result", normalizeResult(parsed));
            }
            return parsed;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call knowledge HTTP endpoint", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling knowledge HTTP endpoint", e);
        }
    }

    /**
     * 将响应中的各类字段（result / data / documents）规范化为统一的结果字符串。
     * <p>
     * Normalize various response fields (result / data / documents) into a unified result string.
     *
     * @param parsed 已解析的响应映射 / parsed response map
     * @return 规范化的结果字符串 / normalized result string
     */
    private String normalizeResult(Map<String, Object> parsed) {
        Object result = parsed.get("result");
        if (result != null) {
            return String.valueOf(result);
        }
        Object data = parsed.get("data");
        if (data != null) {
            return String.valueOf(data);
        }
        Object documents = parsed.get("documents");
        if (documents != null) {
            return String.valueOf(documents);
        }
        return String.valueOf(parsed);
    }

    /**
     * 安全地将 Object 转换为 Map&lt;String, Object&gt;。
     * <p>
     * Safely cast Object to Map&lt;String, Object&gt;.
     *
     * @param value 待转换的值 / value to cast
     * @return 转换后的 Map 或空 Map / cast map or empty map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
