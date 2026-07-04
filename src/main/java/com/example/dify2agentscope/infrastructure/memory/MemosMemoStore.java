package com.example.dify2agentscope.infrastructure.memory;

import com.example.dify2agentscope.domain.memory.MemoRecord;
import com.example.dify2agentscope.domain.memory.MemoSearchRequest;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.memory.MemoWriteRequest;
import com.example.dify2agentscope.domain.workflow.RuntimeConfig;
import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Memos HTTP API 的长期记忆存储实现。
 * <p>负责把项目内的记忆写入/搜索请求转换为 Memos v1 API 调用，并统一转换为领域层 {@link MemoRecord}。</p>
 */
public class MemosMemoStore implements MemoStore {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DifyRuntimeProperties.Memos memos;
    private final RuntimeConfig runtimeConfig;
    private final HttpClient httpClient;

    /**
     * 创建 Memos 长期记忆存储。
     *
     * @param memos         Memos 连接配置
     * @param runtimeConfig 通用运行时配置
     */
    public MemosMemoStore(DifyRuntimeProperties.Memos memos, RuntimeConfig runtimeConfig) {
        this.memos = memos;
        this.runtimeConfig = runtimeConfig;
        this.httpClient = HttpClient.newBuilder().connectTimeout(runtimeConfig.requestTimeout()).build();
    }

    /**
     * 写入一条长期记忆。
     *
     * @param request 记忆写入请求
     * @return Memos 返回的记忆记录
     */
    @Override
    public MemoRecord save(MemoWriteRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", request.content());
        payload.put("visibility", request.visibility() == null || request.visibility().isBlank()
                ? memos.getVisibility()
                : request.visibility());
        payload.put("state", "NORMAL");
        return toRecord(send("POST", endpoint("/api/v1/memos"), payload));
    }

    /**
     * 按文本内容检索长期记忆。
     *
     * @param request 记忆检索请求
     * @return 匹配的记忆列表
     */
    @Override
    public List<MemoRecord> search(MemoSearchRequest request) {
        int limit = request.limit() <= 0 ? memos.getDefaultLimit() : request.limit();
        String filter = "content.contains(\"" + escapeFilter(request.query()) + "\")";
        URI uri = endpoint("/api/v1/memos?pageSize=" + limit + "&filter=" + encode(filter));
        Map<String, Object> response = send("GET", uri, null);
        Object memosValue = response.get("memos");
        if (!(memosValue instanceof List<?> list)) {
            return List.of();
        }
        List<MemoRecord> records = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                records.add(toRecord(castMap(map)));
            }
        }
        return records;
    }

    /**
     * 发送 Memos HTTP 请求并解析 JSON 响应。
     *
     * @param method  HTTP 方法
     * @param uri     请求地址
     * @param payload 请求体，GET 时为 null
     * @return 响应 JSON Map
     */
    private Map<String, Object> send(String method, URI uri, Map<String, Object> payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(runtimeConfig.requestTimeout())
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + memos.getAccessToken());
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload)));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Memos HTTP " + response.statusCode() + ": " + response.body());
            }
            if (response.body() == null || response.body().isBlank()) {
                return Map.of();
            }
            return JSON.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Memos API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Memos API", e);
        }
    }

    /**
     * 将 Memos 响应 Map 转换为领域记忆记录。
     *
     * @param map Memos 响应对象
     * @return 领域记忆记录
     */
    private MemoRecord toRecord(Map<String, Object> map) {
        return new MemoRecord(
                String.valueOf(map.getOrDefault("name", "")),
                String.valueOf(map.getOrDefault("content", "")),
                map.get("tags") instanceof List<?> tags ? tags.stream().map(String::valueOf).toList() : List.of(),
                String.valueOf(map.getOrDefault("visibility", "")),
                parseInstant(map.get("createTime")));
    }

    /**
     * 解析 Memos 时间字段。
     *
     * @param value 原始时间字段
     * @return 解析后的时间，解析失败时返回 null
     */
    private Instant parseInstant(Object value) {
        try {
            return value == null ? null : Instant.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 拼接 Memos API 地址。
     *
     * @param path API 路径
     * @return 完整 URI
     */
    private URI endpoint(String path) {
        return memos.getEndpoint().resolve(path);
    }

    /**
     * 对 URL 查询参数进行编码。
     *
     * @param value 原始字符串
     * @return URL 编码后的字符串
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 转义 Memos filter 表达式中的特殊字符。
     *
     * @param value 原始查询词
     * @return 可安全放入 filter 的查询词
     */
    private String escapeFilter(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 将通配 Map 转为字符串键 Map。
     *
     * @param map 原始 Map
     * @return 字符串键 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
