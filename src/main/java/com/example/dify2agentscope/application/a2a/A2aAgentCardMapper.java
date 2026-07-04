package com.example.dify2agentscope.application.a2a;

import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.domain.a2a.A2aAgentCard;
import com.example.dify2agentscope.domain.a2a.A2aSkill;
import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 WorkflowRuntimeBundle 转换为 A2aAgentCard，供 A2a 协议对外暴露 Agent 元信息。
 * Maps WorkflowRuntimeBundle to A2aAgentCard for exposing Agent metadata via the A2a protocol.
 */
public class A2aAgentCardMapper {

    /**
     * 将工作流运行时包转换为 A2a Agent 卡片。
     * Converts a workflow runtime bundle to an A2a Agent card.
     *
     * @param bundle 工作流运行时包 / workflow runtime bundle
     * @return A2a Agent 卡片 / A2a Agent card
     */
    public A2aAgentCard toAgentCard(WorkflowRuntimeBundle bundle) {
        WorkflowPlan plan = bundle.plan();
        List<A2aSkill> skills = plan.agents().stream()
                .map(this::toSkill)
                .toList();
        return new A2aAgentCard(
                bundle.workflowId(),
                plan.appName(),
                description(plan),
                "1.0.0",
                skills,
                inputSchema(),
                outputSchema(),
                Map.of(
                        "mode", plan.mode(),
                        "nodes", plan.nodes().size(),
                        "edges", plan.edges().size()));
    }

    /**
     * 将 AgentSpec 转换为 A2aSkill。
     * Converts an AgentSpec to an A2aSkill.
     *
     * @param agent Agent 规格 / agent specification
     * @return A2a 技能 / A2a skill
     */
    private A2aSkill toSkill(AgentSpec agent) {
        return new A2aSkill(
                agent.nodeId(),
                agent.title(),
                firstLine(agent.instruction()),
                agent.tools().stream().map(ToolSpec::name).toList());
    }

    /**
     * 从工作流计划中提取描述（优先使用 openingStatement）。
     * Extracts the description from a workflow plan, preferring the opening statement.
     *
     * @param plan 工作流计划 / workflow plan
     * @return 描述文本 / description text
     */
    private String description(WorkflowPlan plan) {
        if (plan.openingStatement() != null && !plan.openingStatement().isBlank()) {
            return firstLine(plan.openingStatement());
        }
        return plan.appName();
    }

    /**
     * 获取多行文本的第一行（去除首尾空白）。
     * Gets the first line of a multi-line text (trimmed).
     *
     * @param value 多行文本 / multi-line text
     * @return 第一行 / first line
     */
    private String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.strip();
        int index = trimmed.indexOf('\n');
        return index < 0 ? trimmed : trimmed.substring(0, index).strip();
    }

    /**
     * 生成 A2a Agent 的输入 JSON Schema。
     * Generates the input JSON Schema for the A2a Agent.
     *
     * @return 输入 Schema / input schema
     */
    private Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("input", Map.of("type", "string", "description", "User query"));
        properties.put("metadata", Map.of("type", "object", "description", "x_dify_chat_id, x_menu_id, user_id"));
        properties.put("conversation", Map.of("type", "object", "description", "Conversation state"));
        return Map.of(
                "type", "object",
                "required", List.of("agentId", "input"),
                "properties", properties);
    }

    /**
     * 生成 A2a Agent 的输出 JSON Schema。
     * Generates the output JSON Schema for the A2a Agent.
     *
     * @return 输出 Schema / output schema
     */
    private Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "answer", Map.of("type", "string"),
                        "conversation", Map.of("type", "object"),
                        "metadata", Map.of("type", "object")));
    }
}
