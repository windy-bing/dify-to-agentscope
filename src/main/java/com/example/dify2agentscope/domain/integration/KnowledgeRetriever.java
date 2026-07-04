package com.example.dify2agentscope.domain.integration;

import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import java.util.Map;

/**
 * Dify 知识检索节点的出站边界。<br>
 * Outbound boundary for Dify knowledge retrieval nodes.
 * 不同部署可以在同一工作流 API 后替换为 HTTP、数据库或测试桩实现。
 * Different deployments can replace the implementation with HTTP, database,
 * or test stubs behind the same workflow API.
 */
public interface KnowledgeRetriever {

    /**
     * 执行知识检索并返回结果映射。<br>
     * Perform knowledge retrieval and return the result map.
     *
     * @param node    Dify 节点定义 / Dify node definition
     * @param context 工作流执行上下文 / workflow execution context
     * @return 检索结果键值对 / key-value pairs of retrieval results
     */
    Map<String, Object> retrieve(DifyNode node, WorkflowExecutionContext context);
}
