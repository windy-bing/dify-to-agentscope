package com.example.dify2agentscope.adapter.in.web;

import com.example.dify2agentscope.domain.nacos.NacosResourceCatalog;
import com.example.dify2agentscope.domain.nacos.NacosResourceItem;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nacos 资源配置控制器 —— 暴露 Nacos 中管理的提示词、技能与 MCP 服务等资源。
 * Nacos resource configuration controller — exposes prompts, skills,
 * and MCP server resources managed in Nacos.
 */
@RestController
@RequestMapping("/api/v1/nacos")
public class NacosResourceController {

    private final NacosResourceCatalog catalog;

    /**
     * 构造 Nacos 资源控制器。
     * Constructs the Nacos resource controller.
     *
     * @param catalog Nacos 资源目录 / Nacos resource catalog
     */
    public NacosResourceController(NacosResourceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 获取所有 Nacos 托管资源的概览（提示词、技能、MCP 服务）。
     * Get an overview of all Nacos-managed resources (prompts, skills, MCP servers).
     *
     * @return 各类资源摘要 / summary of each resource category
     */
    @GetMapping("/resources")
    public Map<String, Object> resources() {
        return Map.of(
                "prompts", summary(catalog.prompts()),
                "skills", summary(catalog.skills()),
                "mcpServers", summary(catalog.mcpServers()));
    }

    /**
     * 生成资源摘要：数量与名称列表。
     * Generate a resource summary: count and names.
     *
     * @param items 资源条目 Map / resource item map
     * @return 摘要信息 / summary information
     */
    private Map<String, Object> summary(Map<String, NacosResourceItem> items) {
        return Map.of(
                "count", items.size(),
                "names", items.keySet());
    }
}
