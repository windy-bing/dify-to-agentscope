package com.example.dify2agentscope.infrastructure.nacos;

import com.example.dify2agentscope.domain.nacos.NacosResourceCatalog;
import com.example.dify2agentscope.domain.nacos.NacosResourceItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * NacosResourceCatalog 的内存实现，使用 LinkedHashMap 存储 prompts / skills / mcpServers。
 * <p>
 * In-memory implementation of NacosResourceCatalog, storing prompts / skills / mcpServers in LinkedHashMap.
 */
public class InMemoryNacosResourceCatalog implements NacosResourceCatalog {

    private final Map<String, NacosResourceItem> prompts;
    private final Map<String, NacosResourceItem> skills;
    private final Map<String, NacosResourceItem> mcpServers;

    /**
     * 创建内存资源目录。
     * <p>
     * Create in-memory resource catalog.
     *
     * @param prompts    Prompt 资源映射 / prompt resource map
     * @param skills     Skill 资源映射 / skill resource map
     * @param mcpServers MCP Server 资源映射 / MCP Server resource map
     */
    public InMemoryNacosResourceCatalog(
            Map<String, NacosResourceItem> prompts,
            Map<String, NacosResourceItem> skills,
            Map<String, NacosResourceItem> mcpServers) {
        this.prompts = new LinkedHashMap<>(prompts);
        this.skills = new LinkedHashMap<>(skills);
        this.mcpServers = new LinkedHashMap<>(mcpServers);
    }

    /**
     * 创建空的资源目录。
     * <p>
     * Create an empty resource catalog.
     *
     * @return 空目录 / empty catalog
     */
    public static InMemoryNacosResourceCatalog empty() {
        return new InMemoryNacosResourceCatalog(Map.of(), Map.of(), Map.of());
    }

    /**
     * 按 key 查询 Prompt 资源。
     *
     * @param key Prompt key
     * @return Prompt 资源，可为空
     */
    @Override
    public Optional<NacosResourceItem> prompt(String key) {
        return Optional.ofNullable(prompts.get(key));
    }

    /**
     * 按名称查询 Skill 资源。
     *
     * @param name Skill 名称
     * @return Skill 资源，可为空
     */
    @Override
    public Optional<NacosResourceItem> skill(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    /**
     * 按名称查询 MCP Server 资源。
     *
     * @param name MCP Server 名称
     * @return MCP Server 资源，可为空
     */
    @Override
    public Optional<NacosResourceItem> mcpServer(String name) {
        return Optional.ofNullable(mcpServers.get(name));
    }

    /**
     * 获取全部 Prompt 资源快照。
     *
     * @return Prompt 资源 Map 的不可变副本
     */
    @Override
    public Map<String, NacosResourceItem> prompts() {
        return Map.copyOf(prompts);
    }

    /**
     * 获取全部 Skill 资源快照。
     *
     * @return Skill 资源 Map 的不可变副本
     */
    @Override
    public Map<String, NacosResourceItem> skills() {
        return Map.copyOf(skills);
    }

    /**
     * 获取全部 MCP Server 资源快照。
     *
     * @return MCP Server 资源 Map 的不可变副本
     */
    @Override
    public Map<String, NacosResourceItem> mcpServers() {
        return Map.copyOf(mcpServers);
    }
}
