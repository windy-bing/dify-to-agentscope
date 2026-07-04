package com.example.dify2agentscope.domain.workflow;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果。
 * <p>包含最终答案、对话变量状态、各节点输出、执行路径及链路 traceId。
 * 无论执行成功或失败都返回完整字段，由调用方根据 {@code success} 决定如何展示。
 *
 * @param answer        最终回答文本，由 answer 节点或 Agent 节点生成
 * @param conversation  执行结束后的对话变量快照
 * @param nodeOutputs   节点输出集合，key=节点 ID，value=节点输出 Map
 * @param executionPath 按序经过的节点 ID 列表
 * @param traceId       链路追踪 ID
 * @param success       是否成功执行完毕
 * @param error         错误信息，仅 success 为 false 时有意义
 */
public record WorkflowResponse(
        String answer,
        Map<String, Object> conversation,
        Map<String, Map<String, Object>> nodeOutputs,
        List<String> executionPath,
        String traceId,
        boolean success,
        String error) {
}
