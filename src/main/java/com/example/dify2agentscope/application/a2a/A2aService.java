package com.example.dify2agentscope.application.a2a;

import com.example.dify2agentscope.application.workflow.WorkflowRegistry;
import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.domain.a2a.A2aAgentCard;
import com.example.dify2agentscope.domain.a2a.A2aTask;
import com.example.dify2agentscope.domain.a2a.A2aTaskRequest;
import com.example.dify2agentscope.domain.a2a.A2aTaskResult;
import com.example.dify2agentscope.domain.a2a.A2aTaskStatus;
import com.example.dify2agentscope.domain.a2a.A2aTaskStore;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * A2a 协议服务，提供 Agent 列表查询、Agent 详情查询和任务创建能力。
 * A2a protocol service providing Agent listing, Agent detail query, and task creation capabilities.
 */
@Service
public class A2aService {

    private final WorkflowRegistry workflowRegistry;
    private final A2aTaskStore taskStore;
    private final A2aAgentCardMapper agentCardMapper = new A2aAgentCardMapper();

    /**
     * 构造 A2a 服务。
     * Constructs the A2a service.
     *
     * @param workflowRegistry 工作流注册表 / workflow registry
     * @param taskStore        A2a 任务存储 / A2a task store
     */
    public A2aService(WorkflowRegistry workflowRegistry, A2aTaskStore taskStore) {
        this.workflowRegistry = workflowRegistry;
        this.taskStore = taskStore;
    }

    /**
     * 返回所有可用的 Agent 卡片列表。
     * Returns the list of all available Agent cards.
     *
     * @return Agent 卡片列表 / list of Agent cards
     */
    public List<A2aAgentCard> listAgents() {
        return workflowRegistry.workflowIds().stream()
                .map(workflowRegistry::get)
                .map(agentCardMapper::toAgentCard)
                .toList();
    }

    /**
     * 根据 agentId 获取单个 Agent 卡片详情。
     * Gets a single Agent card detail by agentId.
     *
     * @param agentId Agent 标识 / Agent identifier
     * @return Agent 卡片 / Agent card
     */
    public A2aAgentCard getAgent(String agentId) {
        return agentCardMapper.toAgentCard(workflowRegistry.get(agentId));
    }

    /**
     * 创建 A2a 任务并同步执行工作流，返回最终任务状态。
     * Creates an A2a task, executes the workflow synchronously, and returns the final task status.
     *
     * @param request A2a 任务请求 / A2a task request
     * @return 已完成的 A2a 任务 / completed A2a task
     */
    public A2aTask createTask(A2aTaskRequest request) {
        WorkflowRuntimeBundle bundle = workflowRegistry.get(request.getAgentId());
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        A2aTask submitted = new A2aTask(
                taskId,
                request.getAgentId(),
                A2aTaskStatus.SUBMITTED,
                null,
                "",
                now,
                now,
                Map.of("workflowId", request.getAgentId()));
        taskStore.save(submitted);

        A2aTask running = withStatus(submitted, A2aTaskStatus.RUNNING, null, "");
        taskStore.save(running);

        WorkflowResponse workflowResponse = bundle.executor().execute(toWorkflowRequest(request));
        A2aTask completed = workflowResponse.success()
                ? completedTask(running, workflowResponse)
                : failedTask(running, workflowResponse.error());
        taskStore.save(completed);
        return completed;
    }

    /**
     * 根据 taskId 查询任务状态和结果。
     * Queries the task status and result by taskId.
     *
     * @param taskId 任务标识 / task identifier
     * @return A2a 任务 / A2a task
     * @throws IllegalArgumentException 未知的 taskId / unknown taskId
     */
    public A2aTask getTask(String taskId) {
        return taskStore.find(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown taskId: " + taskId));
    }

    /**
     * 将 A2aTaskRequest 转换为内部 WorkflowRequest。
     * Converts an A2aTaskRequest to an internal WorkflowRequest.
     *
     * @param request A2a 任务请求 / A2a task request
     * @return 工作流请求 / workflow request
     */
    private WorkflowRequest toWorkflowRequest(A2aTaskRequest request) {
        Map<String, Object> metadata = request.getMetadata();
        return new WorkflowRequest(
                request.getInput(),
                stringValue(metadata.getOrDefault("x_dify_chat_id", metadata.get("conversation_id"))),
                stringValue(metadata.getOrDefault("x_menu_id", "a2a")),
                stringValue(metadata.getOrDefault("user_id", metadata.get("userId"))),
                request.getConversation(),
                stringValue(metadata.getOrDefault("trace_id", metadata.get("traceId"))));
    }

    /**
     * 根据工作流响应构建已完成状态的 A2a 任务。
     * Builds a COMPLETED status A2a task from a workflow response.
     *
     * @param task     原始任务 / original task
     * @param response 工作流响应 / workflow response
     * @return 已完成的任务 / completed task
     */
    private A2aTask completedTask(A2aTask task, WorkflowResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>(task.metadata());
        metadata.put("executionPath", response.executionPath());
        metadata.put("traceId", response.traceId());
        A2aTaskResult result = new A2aTaskResult(
                response.answer(),
                response.conversation(),
                Map.of("nodeOutputs", response.nodeOutputs()),
                metadata);
        return withStatus(task, A2aTaskStatus.COMPLETED, result, "");
    }

    /**
     * 构建失败状态的 A2a 任务。
     * Builds a FAILED status A2a task.
     *
     * @param task  原始任务 / original task
     * @param error 错误信息 / error message
     * @return 失败的任务 / failed task
     */
    private A2aTask failedTask(A2aTask task, String error) {
        return withStatus(task, A2aTaskStatus.FAILED, null, error);
    }

    /**
     * 创建指定状态的新 A2a 任务副本。
     * Creates a new A2a task copy with the specified status.
     *
     * @param task   原始任务 / original task
     * @param status 新状态 / new status
     * @param result 任务结果 / task result
     * @param error  错误信息 / error message
     * @return 更新状态后的任务 / task with updated status
     */
    private A2aTask withStatus(A2aTask task, A2aTaskStatus status, A2aTaskResult result, String error) {
        return new A2aTask(
                task.id(),
                task.agentId(),
                status,
                result,
                error,
                task.createdAt(),
                Instant.now(),
                task.metadata());
    }

    /**
     * 将对象安全转换为字符串，null 时返回空字符串。
     * Safely converts an object to a string, returning empty string for null.
     *
     * @param value 待转换对象 / object to convert
     * @return 字符串值 / string value
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
