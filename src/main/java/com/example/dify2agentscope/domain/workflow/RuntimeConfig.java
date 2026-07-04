package com.example.dify2agentscope.domain.workflow;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流执行器运行时配置。
 * <p>包含 Higress MCP 网关、知识检索服务以及全局请求超时等参数，
 * 由 {@code RuntimeConfigFactory} 统一构建，向所有工作流执行提供基础设施连接信息。
 *
 * @param higressMcpEndpoint    Higress MCP 网关地址，用于 Agent 节点工具调用转发
 * @param higressMcpBearerToken Higress MCP 网关的 Bearer token
 * @param knowledgeEndpoint     知识检索服务地址，用于 knowledge-retrieval 节点调用
 * @param knowledgeBearerToken  知识检索服务的 Bearer token
 * @param requestTimeout        所有出站 HTTP 请求的超时时间
 * @param defaultNodeTimeout    单个工作流节点默认最大执行时间
 * @param nodeTimeouts          指定节点 ID 或节点类型的最大执行时间覆盖
 * @param extraHeaders          附加到所有出站请求的 HTTP 头
 */
public record RuntimeConfig(
        Optional<URI> higressMcpEndpoint,
        Optional<String> higressMcpBearerToken,
        Optional<URI> knowledgeEndpoint,
        Optional<String> knowledgeBearerToken,
        Duration requestTimeout,
        Duration defaultNodeTimeout,
        Map<String, Duration> nodeTimeouts,
        Map<String, String> extraHeaders) {
}
