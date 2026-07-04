package com.example.dify2agentscope.domain.workflow;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流执行请求。
 * <p>封装用户输入的 query、会话标识、菜单标识、用户 ID、对话变量及链路 traceId。
 * 由 HTTP 入口或 A2A 协议解析后构造，作为 {@link WorkflowExecutor#execute} 的入参。
 *
 * @param query       用户原始输入
 * @param xDifyChatId Dify 侧会话 ID，用于跨请求上下文关联
 * @param xMenuId     菜单 / 技能 ID，标识当前所属功能模块
 * @param userId      用户标识
 * @param conversation 对话变量 Map，执行过程中可被 assigner 节点修改
 * @param traceId     链路追踪 ID，未传入时由执行上下文自动生成
 */
public record WorkflowRequest(
        String query,
        String xDifyChatId,
        String xMenuId,
        String userId,
        Map<String, Object> conversation,
        String traceId) {

    /**
     * 规范构造器，对 {@code conversation} 执行防御性拷贝。
     * <p>若入参为 {@code null} 则初始化为空 {@link LinkedHashMap}，保证内部集合可变且不受调用方后续修改影响。
     */
    public WorkflowRequest {
        conversation = conversation == null ? new LinkedHashMap<>() : new LinkedHashMap<>(conversation);
    }

    /**
     * 便捷构造器，省略 {@code traceId} 参数。
     * <p>traceId 默认设为空字符串，由执行上下文在缺少时自动填充 UUID。
     *
     * @param query       用户原始输入
     * @param xDifyChatId Dify 侧会话 ID
     * @param xMenuId     菜单 / 技能 ID
     * @param userId      用户标识
     * @param conversation 对话变量 Map
     */
    public WorkflowRequest(
            String query,
            String xDifyChatId,
            String xMenuId,
            String userId,
            Map<String, Object> conversation) {
        this(query, xDifyChatId, xMenuId, userId, conversation, "");
    }
}
