package com.example.dify2agentscope.domain.dify;

import java.util.List;

/**
 * Dify DSL 解析后的完整工作流计划，是不可变模型。
 * <p>包含应用元信息、变量定义、图结构（节点+边）及 Agent 规格说明。
 * 整个工作流的执行以此计划为输入，不直接依赖原始 DSL 格式。
 */
public record WorkflowPlan(
        String appName,
        String mode,
        String openingStatement,
        List<VariableSpec> variables,
        List<DifyNode> nodes,
        List<DifyEdge> edges,
        List<AgentSpec> agents) {
}
