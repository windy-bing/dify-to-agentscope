package com.example.dify2agentscope.domain.workflow;

import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单个 Dify 工作流计划编译后的运行时对象。
 * <p>将工作流图结构（节点 + 边）索引化，提供按 ID 查节点、按 source 查下游边的快速访问。
 * 每个 DSL 导入后创建一个实例，由 {@link com.example.dify2agentscope.application.workflow.WorkflowRegistry} 按 workflowId 管理。
 */
public class WorkflowRuntime {

    /** 原始工作流计划（不可变模型） */
    private final WorkflowPlan plan;

    /** 节点索引：nodeId → DifyNode */
    private final Map<String, DifyNode> nodesById;

    /** 出边索引：source nodeId → 下游边列表 */
    private final Map<String, List<DifyEdge>> outgoingEdges;

    /**
     * 根据工作流计划构建运行时。
     * <p>遍历节点集和边集，建立 ID 索引与出边索引，供执行器在调度过程中 O(1) 查询。
     *
     * @param plan 已解析的工作流计划
     */
    public WorkflowRuntime(WorkflowPlan plan) {
        this.plan = plan;
        this.nodesById = plan.nodes().stream()
                .collect(Collectors.toMap(DifyNode::id, node -> node));
        this.outgoingEdges = plan.edges().stream()
                .collect(Collectors.groupingBy(DifyEdge::source));
    }

    /**
     * 返回原始工作流计划。
     *
     * @return 不可变的工作流计划对象
     */
    public WorkflowPlan plan() {
        return plan;
    }

    /**
     * 按节点 ID 获取工作流节点。
     *
     * @param nodeId 节点唯一标识
     * @return 对应的节点对象，不存在时返回 {@code null}
     */
    public DifyNode node(String nodeId) {
        return nodesById.get(nodeId);
    }

    /**
     * 获取指定节点的所有下游出边。
     * <p>用于 if-else / switch 等分支节点确定下一步流转方向。
     *
     * @param nodeId 当前节点 ID
     * @return 下游边列表，无出边时返回空列表
     */
    public List<DifyEdge> nextEdges(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, List.of());
    }
}
