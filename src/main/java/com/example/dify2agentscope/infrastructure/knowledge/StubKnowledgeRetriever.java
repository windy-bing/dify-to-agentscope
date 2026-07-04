package com.example.dify2agentscope.infrastructure.knowledge;

import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KnowledgeRetriever 的桩实现，返回模拟结果，用于开发或测试阶段。
 * <p>
 * Stub implementation of KnowledgeRetriever that returns mock results, used during development or testing.
 */
public class StubKnowledgeRetriever implements KnowledgeRetriever {
    /**
     * 返回空知识检索结果。
     *
     * @param node    当前知识检索节点
     * @param context 工作流执行上下文
     * @return 标记为 stub 的检索结果
     */
    @Override
    public Map<String, Object> retrieve(DifyNode node, WorkflowExecutionContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", "");
        result.put("status", "stub");
        result.put("node", node.title());
        return result;
    }
}
