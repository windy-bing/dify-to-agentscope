package com.example.dify2agentscope.domain.nacos;

import java.time.Instant;
import java.util.Map;

/**
 * Nacos 资源项。
 * <p>
 * 代表从 Nacos 配置中心加载的一条资源记录，包含名称、类型、内容及元数据。
 * Nacos resource item — represents a single resource record loaded from the Nacos configuration
 * center, including its name, type, content, and metadata.
 *
 * @param name     资源名称 / resource name
 * @param type     资源类型 / resource type
 * @param content  文本内容 / text content
 * @param bytes    二进制内容 / binary content
 * @param metadata 附加元数据 / additional metadata
 * @param loadedAt 加载时间 / load timestamp
 */
public record NacosResourceItem(
        String name,
        NacosResourceType type,
        String content,
        byte[] bytes,
        Map<String, Object> metadata,
        Instant loadedAt) {

    /**
     * 构造 Nacos 资源项，对元数据进行防御性拷贝。
     * Constructs a Nacos resource item, defensively copying the metadata map and cloning
     * the byte array.
     */
    public NacosResourceItem {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        bytes = bytes == null ? null : bytes.clone();
    }
}
