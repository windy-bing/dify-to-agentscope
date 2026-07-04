package com.example.dify2agentscope.domain.agentscope;

import java.util.Map;

/**
 * AgentScope 运行时节点模型。
 * <p>该模型不携带 Dify 命名；无论来源是 Dify DSL、手写 YAML 还是 HTTP 模板，进入运行时后都应该归一到这里。</p>
 *
 * @param id       节点 ID
 * @param kind     AgentScope 语义节点类型，例如 start、agent、tool、knowledge、memory、answer
 * @param title    节点标题
 * @param settings 节点配置
 */
public record AgentScopeNode(
        String id,
        String kind,
        String title,
        Map<String, Object> settings) {
}
