package com.example.dify2agentscope.domain.agentscope;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.VariableSpec;
import java.util.List;

/**
 * AgentScope 规范化 workflow 计划。
 * <p>这是本项目运行时的目标模型。Dify DSL 只允许作为输入格式存在，解析完成后必须转换为该模型；
 * DAG 字段保留是为了迁移兼容，自循环和 Harness 模式则逐步对齐 AgentScope 官方能力。</p>
 *
 * @param appName       应用名称
 * @param executionMode 执行模式
 * @param opening       开场白
 * @param variables     对话变量
 * @param nodes         AgentScope 语义节点
 * @param edges         DAG 兼容模式边
 * @param agents        ReActAgent 构建规格
 */
public record AgentScopeWorkflowPlan(
        String appName,
        AgentScopeExecutionMode executionMode,
        String opening,
        List<VariableSpec> variables,
        List<AgentScopeNode> nodes,
        List<AgentScopeEdge> edges,
        List<AgentSpec> agents) {
}
