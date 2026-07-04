package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.adapter.in.web.ChatRequestDto;
import com.example.dify2agentscope.domain.memory.SessionMemoryStore;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作流编排服务，将外部请求路由到指定工作流执行器执行。
 * Orchestration service that routes external requests to the designated workflow executor.
 */
@Service
public class WorkflowService {

    private final WorkflowRegistry workflowRegistry;
    private final SessionMemoryStore sessionMemoryStore;

    /**
     * 构造工作流服务。
     * Constructs the workflow service.
     *
     * @param workflowRegistry 工作流注册表 / workflow registry
     */
    @Autowired
    public WorkflowService(WorkflowRegistry workflowRegistry, SessionMemoryStore sessionMemoryStore) {
        this.workflowRegistry = workflowRegistry;
        this.sessionMemoryStore = sessionMemoryStore;
    }

    /**
     * 兼容旧测试和简单场景的构造器。
     * <p>未显式注入 session memory 时，默认使用保留 20 轮的内存实现。</p>
     *
     * @param workflowRegistry 工作流注册表
     */
    public WorkflowService(WorkflowRegistry workflowRegistry) {
        this(workflowRegistry, new com.example.dify2agentscope.infrastructure.memory.InMemorySessionMemoryStore(20));
    }

    /**
     * 根据 workflowId 执行对应 Dify 工作流。
     * Executes the Dify workflow identified by the given workflowId.
     *
     * @param workflowId 工作流标识 / workflow identifier
     * @param request    Web 请求 DTO / web request DTO
     * @return 工作流响应 / workflow response
     */
    public WorkflowResponse execute(String workflowId, ChatRequestDto request) {
        WorkflowExecutor executor = workflowRegistry.get(workflowId).executor();
        WorkflowRequest initialRequest = new WorkflowRequest(
                request.getQuery(),
                request.getxDifyChatId(),
                request.getxMenuId(),
                request.getUserId(),
                request.getConversation(),
                request.getTraceId());
        Map<String, Object> conversation = new LinkedHashMap<>(sessionMemoryStore.load(workflowId, initialRequest));
        conversation.putAll(initialRequest.conversation());
        WorkflowRequest mergedRequest = new WorkflowRequest(
                initialRequest.query(),
                initialRequest.xDifyChatId(),
                initialRequest.xMenuId(),
                initialRequest.userId(),
                conversation,
                initialRequest.traceId());
        WorkflowResponse response = executor.execute(mergedRequest);
        sessionMemoryStore.save(workflowId, mergedRequest, response);
        return response;
    }
}
