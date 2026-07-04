package com.example.dify2agentscope.domain.a2a;

import java.util.List;
import java.util.Map;

/**
 * A2A Agent 名片。
 * <p>
 * 描述一个 Agent 的基本信息、所支持的技能列表、输入/输出 Schema 以及自定义元数据。
 * A2A agent card — describes an agent's basic information, the list of skills it supports,
 * its input/output schema, and custom metadata.
 *
 * @param id          Agent 唯一标识 / unique agent identifier
 * @param name        Agent 名称 / agent name
 * @param description Agent 描述 / agent description
 * @param version     Agent 版本号 / agent version
 * @param skills      技能列表 / list of supported skills
 * @param inputSchema 输入参数 Schema / input parameter schema
 * @param outputSchema 输出参数 Schema / output parameter schema
 * @param metadata    附加元数据 / additional metadata
 */
public record A2aAgentCard(
        String id,
        String name,
        String description,
        String version,
        List<A2aSkill> skills,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> metadata) {
}
