package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.domain.agentscope.AgentScopeEdge;
import com.example.dify2agentscope.domain.agentscope.AgentScopeExecutionMode;
import com.example.dify2agentscope.domain.agentscope.AgentScopeNode;
import com.example.dify2agentscope.domain.agentscope.AgentScopeWorkflowPlan;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将任意入口来源的 legacy WorkflowPlan 转换为 AgentScope 规范模型。
 * <p>当前 WorkflowPlan 名称仍保留 Dify 历史包名，是迁移期兼容模型；本 Mapper 是边界收敛点，
 * 后续执行器应逐步改为直接消费 {@link AgentScopeWorkflowPlan}。</p>
 */
public class AgentScopeWorkflowPlanMapper {

    /**
     * 转换为 AgentScope 规范 workflow。
     *
     * @param legacyPlan Dify DSL 或手写 YAML 解析得到的兼容计划
     * @return AgentScope 规范计划
     */
    public AgentScopeWorkflowPlan toAgentScope(WorkflowPlan legacyPlan) {
        return new AgentScopeWorkflowPlan(
                legacyPlan.appName(),
                executionMode(legacyPlan),
                legacyPlan.openingStatement(),
                legacyPlan.variables(),
                legacyPlan.nodes().stream().map(this::toNode).toList(),
                legacyPlan.edges().stream().map(this::toEdge).toList(),
                legacyPlan.agents());
    }

    /**
     * 将 legacy 节点转换为 AgentScope 语义节点。
     *
     * @param node legacy 节点
     * @return AgentScope 节点
     */
    private AgentScopeNode toNode(DifyNode node) {
        Map<String, Object> settings = new LinkedHashMap<>();
        if (node.data() instanceof Map<?, ?> map) {
            map.forEach((key, value) -> settings.put(String.valueOf(key), value));
        }
        settings.putIfAbsent("legacyType", node.type());
        return new AgentScopeNode(node.id(), toAgentScopeKind(node.type()), node.title(), settings);
    }

    /**
     * 将 legacy 边转换为 AgentScope DAG 兼容边。
     *
     * @param edge legacy 边
     * @return AgentScope 边
     */
    private AgentScopeEdge toEdge(DifyEdge edge) {
        return new AgentScopeEdge(edge.id(), edge.source(), edge.sourceHandle(), edge.target());
    }

    /**
     * 判断 workflow 的目标执行模式。
     *
     * @param plan legacy 计划
     * @return AgentScope 执行模式
     */
    private AgentScopeExecutionMode executionMode(WorkflowPlan plan) {
        if ("agentscope-react".equalsIgnoreCase(plan.mode()) || "react-self-loop".equalsIgnoreCase(plan.mode())) {
            return AgentScopeExecutionMode.REACT_SELF_LOOP;
        }
        if ("agentscope-harness".equalsIgnoreCase(plan.mode())) {
            return AgentScopeExecutionMode.HARNESS_MULTI_AGENT;
        }
        return AgentScopeExecutionMode.DAG;
    }

    /**
     * 将 Dify/legacy 节点类型映射为 AgentScope 语义类型。
     *
     * @param legacyType legacy 节点类型
     * @return AgentScope 语义类型
     */
    private String toAgentScopeKind(String legacyType) {
        if (legacyType == null || legacyType.isBlank()) {
            return "unknown";
        }
        return switch (legacyType) {
            case "knowledge-retrieval" -> "knowledge";
            case "memo-retrieval" -> "memory";
            case "if-else" -> "router";
            case "assigner" -> "state-update";
            case "code" -> "code";
            case "answer" -> "answer";
            case "agent" -> "agent";
            case "start" -> "start";
            default -> legacyType;
        };
    }
}
