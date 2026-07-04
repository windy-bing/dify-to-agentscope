package com.example.dify2agentscope.infrastructure.memory;

import com.example.dify2agentscope.domain.memory.SessionMemoryStore;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import java.util.Map;

/**
 * 关闭短期 session memory 时使用的空实现。
 * <p>加载时返回空上下文，保存时不做任何操作。</p>
 */
public class NoOpSessionMemoryStore implements SessionMemoryStore {

    /**
     * 返回空的会话状态。
     *
     * @param workflowId workflow ID
     * @param request    当前请求
     * @return 空 Map
     */
    @Override
    public Map<String, Object> load(String workflowId, WorkflowRequest request) {
        return Map.of();
    }

    /**
     * 忽略会话状态保存。
     *
     * @param workflowId workflow ID
     * @param request    当前请求
     * @param response   工作流响应
     */
    @Override
    public void save(String workflowId, WorkflowRequest request, WorkflowResponse response) {
        // Disabled by configuration.
    }
}
