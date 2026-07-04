package com.example.dify2agentscope.domain.nacos;

import java.util.Map;
import java.util.Optional;

/**
 * Nacos 资源目录。
 * <p>
 * workflow 运行时只读取该接口，避免直接依赖 Nacos SDK 或具体注册中心实现。
 * Nacos resource catalog — the workflow runtime reads only this interface to avoid a direct
 * dependency on the Nacos SDK or any specific service registry implementation.
 */
public interface NacosResourceCatalog {

    /**
     * 根据键查找提示词资源。
     * Looks up a prompt resource by key.
     *
     * @param key 提示词键 / prompt key
     * @return 包含资源项的可选值 / an Optional containing the resource item
     */
    Optional<NacosResourceItem> prompt(String key);

    /**
     * 根据名称查找技能资源。
     * Looks up a skill resource by name.
     *
     * @param name 技能名称 / skill name
     * @return 包含资源项的可选值 / an Optional containing the resource item
     */
    Optional<NacosResourceItem> skill(String name);

    /**
     * 根据名称查找 MCP 服务器资源。
     * Looks up an MCP server resource by name.
     *
     * @param name MCP 服务器名称 / MCP server name
     * @return 包含资源项的可选值 / an Optional containing the resource item
     */
    Optional<NacosResourceItem> mcpServer(String name);

    /**
     * 获取所有提示词资源。
     * Returns all prompt resources.
     *
     * @return 键为资源名、值为资源项的映射 / a map of resource name to resource item
     */
    Map<String, NacosResourceItem> prompts();

    /**
     * 获取所有技能资源。
     * Returns all skill resources.
     *
     * @return 键为资源名、值为资源项的映射 / a map of resource name to resource item
     */
    Map<String, NacosResourceItem> skills();

    /**
     * 获取所有 MCP 服务器资源。
     * Returns all MCP server resources.
     *
     * @return 键为资源名、值为资源项的映射 / a map of resource name to resource item
     */
    Map<String, NacosResourceItem> mcpServers();
}
