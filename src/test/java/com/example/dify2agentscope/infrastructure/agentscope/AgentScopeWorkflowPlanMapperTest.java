package com.example.dify2agentscope.infrastructure.agentscope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.dify2agentscope.domain.agentscope.AgentScopeExecutionMode;
import com.example.dify2agentscope.domain.agentscope.AgentScopeWorkflowPlan;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * AgentScope 规范模型转换测试。
 * <p>确保 Dify/legacy 输入进入运行时前会归一成 AgentScope 语义，不把 Dify 节点类型继续扩散到新模型。</p>
 */
class AgentScopeWorkflowPlanMapperTest {

    /** Dify 节点类型应转换为 AgentScope 语义节点类型 */
    @Test
    void mapsLegacyDifyPlanToAgentScopePlan() {
        WorkflowPlan legacy = new WorkflowPlan(
                "app",
                "advanced-chat",
                "",
                List.of(),
                List.of(
                        new DifyNode("start", "start", "Start", Map.of()),
                        new DifyNode("kb", "knowledge-retrieval", "KB", Map.of()),
                        new DifyNode("route", "if-else", "Route", Map.of())),
                List.of(new DifyEdge("e1", "start", "source", "kb")),
                List.of());

        AgentScopeWorkflowPlan plan = new AgentScopeWorkflowPlanMapper().toAgentScope(legacy);

        assertEquals(AgentScopeExecutionMode.DAG, plan.executionMode());
        assertEquals("start", plan.nodes().get(0).kind());
        assertEquals("knowledge", plan.nodes().get(1).kind());
        assertEquals("router", plan.nodes().get(2).kind());
        assertEquals("knowledge-retrieval", plan.nodes().get(1).settings().get("legacyType"));
    }

    /** 空 legacy type 应落到 unknown，不能因为 switch null 导致映射阶段崩溃 */
    @Test
    void mapsBlankLegacyTypeToUnknownKind() {
        WorkflowPlan legacy = new WorkflowPlan(
                "app",
                "advanced-chat",
                "",
                List.of(),
                List.of(new DifyNode("broken", null, "Broken", Map.of())),
                List.of(),
                List.of());

        AgentScopeWorkflowPlan plan = new AgentScopeWorkflowPlanMapper().toAgentScope(legacy);

        assertEquals("unknown", plan.nodes().get(0).kind());
    }
}
