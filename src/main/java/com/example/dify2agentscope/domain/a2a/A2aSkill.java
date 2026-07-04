package com.example.dify2agentscope.domain.a2a;

import java.util.List;

/**
 * A2A 技能定义。
 * <p>
 * 描述一个 Agent 对外暴露的单个技能，包含唯一标识、名称、描述以及所使用的工具列表。
 * A2A skill definition — describes a single skill exposed by an agent, including its unique
 * identifier, name, description, and the list of tools it uses.
 *
 * @param id          技能唯一标识 / unique skill identifier
 * @param name        技能名称 / skill name
 * @param description 技能描述 / skill description
 * @param tools       技能所用的工具列表 / list of tools used by this skill
 */
public record A2aSkill(
        String id,
        String name,
        String description,
        List<String> tools) {
}
