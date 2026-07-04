package com.example.dify2agentscope.infrastructure.memory;

import com.example.dify2agentscope.domain.memory.SessionMemoryStore;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的短期会话记忆存储。
 * <p>按 workflowId、userId 和 chatId 隔离会话，只用于本地和最小生产形态；进程重启后数据会丢失。</p>
 */
public class InMemorySessionMemoryStore implements SessionMemoryStore {

    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private final int maxTurns;

    /**
     * 创建内存会话记忆存储。
     *
     * @param maxTurns 每个会话最多保留的轮次数
     */
    public InMemorySessionMemoryStore(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    /**
     * 加载指定 workflow 请求对应的会话状态。
     *
     * @param workflowId workflow ID
     * @param request    当前请求
     * @return conversation 变量快照
     */
    @Override
    public Map<String, Object> load(String workflowId, WorkflowRequest request) {
        return new LinkedHashMap<>(sessions.getOrDefault(key(workflowId, request), Map.of()));
    }

    /**
     * 保存成功响应产生的会话状态。
     *
     * @param workflowId workflow ID
     * @param request    当前请求
     * @param response   执行响应
     */
    @Override
    public void save(String workflowId, WorkflowRequest request, WorkflowResponse response) {
        if (!response.success()) {
            return;
        }
        Map<String, Object> state = new LinkedHashMap<>(response.conversation());
        List<Map<String, Object>> turns = previousTurns(state.get("_session_turns"));
        turns.add(Map.of("query", request.query(), "answer", response.answer(), "traceId", response.traceId()));
        while (turns.size() > maxTurns) {
            turns.remove(0);
        }
        state.put("_session_turns", turns);
        state.put("history_user_query", turns.stream().map(turn -> String.valueOf(turn.get("query"))).toList());
        state.put("reply", response.answer());
        sessions.put(key(workflowId, request), state);
    }

    /**
     * 从历史状态中恢复会话轮次列表。
     *
     * @param value 原始历史轮次对象
     * @return 可变的历史轮次列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> previousTurns(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> turns = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    turns.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
            return turns;
        }
        return new ArrayList<>();
    }

    /**
     * 生成会话隔离键。
     *
     * @param workflowId workflow ID
     * @param request    当前请求
     * @return 会话隔离键
     */
    private String key(String workflowId, WorkflowRequest request) {
        return workflowId + ":" + request.userId() + ":" + request.xDifyChatId();
    }
}
