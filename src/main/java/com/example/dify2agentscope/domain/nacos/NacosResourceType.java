package com.example.dify2agentscope.domain.nacos;

/**
 * Nacos 资源类型枚举。
 * <p>
 * 定义了 Nacos 配置中心中支持的资源分类。
 * Enumeration of Nacos resource types — defines the supported resource categories in the
 * Nacos configuration center.
 */
public enum NacosResourceType {
    /** 提示词资源 / Prompt resource */
    PROMPT,
    /** 技能资源 / Skill resource */
    SKILL,
    /** MCP 服务器资源 / MCP server resource */
    MCP_SERVER
}
