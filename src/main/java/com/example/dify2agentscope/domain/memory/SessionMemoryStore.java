package com.example.dify2agentscope.domain.memory;

import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import java.util.Map;

/**
 * 短期会话记忆存储接口。
 * <p>用于在 workflow 执行前加载 conversation 状态，并在执行成功后保存最新对话。</p>
 */
public interface SessionMemoryStore {

    /**
     * 加载指定请求对应的会话状态。
     *
     * @param workflowId workflow ID
     * @param request    当前工作流请求
     * @return conversation 变量快照
     */
    Map<String, Object> load(String workflowId, WorkflowRequest request);

    /**
     * 保存工作流执行后的会话状态。
     *
     * @param workflowId workflow ID
     * @param request    当前工作流请求
     * @param response   工作流响应
     */
    void save(String workflowId, WorkflowRequest request, WorkflowResponse response);
}
