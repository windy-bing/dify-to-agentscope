package com.example.dify2agentscope.adapter.in.web;

import com.example.dify2agentscope.application.workflow.WorkflowRegistry;
import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.application.workflow.WorkflowCreationService;
import com.example.dify2agentscope.domain.agentscope.AgentScopeCapabilityManifest;
import com.example.dify2agentscope.domain.agentscope.AgentScopeOfficialCapability;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.application.workflow.WorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 工作流控制器 — 提供工作流的执行、流式响应与元数据查询接口。
 * Workflow controller — exposes endpoints for workflow execution,
 * streaming responses, and metadata retrieval.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowCreationService workflowCreationService;

    /**
     * 构造工作流控制器。
     * Constructs the workflow controller.
     *
     * @param workflowService 工作流服务 / workflow service
     * @param workflowRegistry 工作流注册表 / workflow registry
     */
    public WorkflowController(
            WorkflowService workflowService,
            WorkflowRegistry workflowRegistry,
            WorkflowCreationService workflowCreationService) {
        this.workflowService = workflowService;
        this.workflowRegistry = workflowRegistry;
        this.workflowCreationService = workflowCreationService;
    }

    /**
     * 通过模板创建并注册一个最小 Agent workflow。
     *
     * @param request 创建请求
     * @return 新 workflow 的元数据
     */
    @PostMapping("/templates/agent")
    public Map<String, Object> createAgentWorkflow(@Valid @RequestBody CreateAgentWorkflowRequest request) {
        WorkflowRuntimeBundle bundle = workflowCreationService.createAgentWorkflow(request);
        return Map.of(
                "workflowId", bundle.workflowId(),
                "appName", bundle.plan().appName(),
                "mode", bundle.plan().mode(),
                "agentScopeExecutionMode", bundle.agentScopePlan() == null
                        ? "DAG"
                        : bundle.agentScopePlan().executionMode().name(),
                "nodes", bundle.plan().nodes().size(),
                "edges", bundle.plan().edges().size(),
                "agents", bundle.plan().agents().size());
    }

    /**
     * 查询当前项目保留的 AgentScope 官方能力清单。
     * <p>该接口用于诊断和后续扩展验收，明确哪些能力已经直接使用官方 API，哪些只是最小适配或扩展占位。</p>
     *
     * @return AgentScope 能力清单
     */
    @GetMapping("/agentscope/capabilities")
    public Map<String, Object> agentScopeCapabilities() {
        List<Map<String, Object>> capabilities = AgentScopeCapabilityManifest.all().stream()
                .map(this::capabilityPayload)
                .toList();
        return Map.of("capabilities", capabilities);
    }

    /**
     * 执行工作流聊天（非流式），返回完整响应。
     * Execute a workflow chat (non-streaming) and return the full response.
     *
     * @param workflowId 工作流 ID / workflow identifier
     * @param request    聊天请求体 / chat request body
     * @return 包含执行结果的响应实体 / response entity with execution result
     */
    @PostMapping("/{workflowId}/chat")
    public ResponseEntity<WorkflowResponse> chat(
            @PathVariable("workflowId") String workflowId,
            @Valid @RequestBody ChatRequestDto request) {
        WorkflowResponse response = workflowService.execute(workflowId, request);
        return ResponseEntity.status(response.success() ? 200 : 422).body(response);
    }

    /**
     * 以 SSE 流式方式执行工作流聊天，逐步推送结果。
     * Execute a workflow chat in SSE streaming fashion, pushing results progressively.
     *
     * @param workflowId 工作流 ID / workflow identifier
     * @param request    聊天请求体 / chat request body
     * @return SSE 发射器 / SSE emitter
     */
    @PostMapping(value = "/{workflowId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable("workflowId") String workflowId,
            @Valid @RequestBody ChatRequestDto request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        try {
            WorkflowResponse response = workflowService.execute(workflowId, request);
            emitter.send(SseEmitter.event().name("answer").data(response.answer()));
            emitter.send(SseEmitter.event().name("workflow").data(response));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * 查询指定工作流的元数据（应用名称、模式、节点数等）。
     * Retrieve metadata for a given workflow (app name, mode, node/edge/agent counts).
     *
     * @param workflowId 工作流 ID / workflow identifier
     * @return 元数据 Map / metadata map
     */
    @GetMapping("/{workflowId}/metadata")
    public Map<String, Object> metadata(@PathVariable("workflowId") String workflowId) {
        WorkflowRuntimeBundle bundle = workflowRegistry.get(workflowId);
        return Map.of(
                "workflowId", workflowId,
                "appName", bundle.plan().appName(),
                "mode", bundle.plan().mode(),
                "agentScopeExecutionMode", bundle.agentScopePlan() == null
                        ? "DAG"
                        : bundle.agentScopePlan().executionMode().name(),
                "nodes", bundle.plan().nodes().size(),
                "edges", bundle.plan().edges().size(),
                "agents", bundle.plan().agents().size());
    }

    /**
     * 获取所有已注册工作流的 ID 列表。
     * Retrieve all registered workflow IDs.
     *
     * @return 工作流 ID 列表 Map / map containing workflow ID list
     */
    @GetMapping
    public Map<String, Object> workflows() {
        return Map.of("workflowIds", workflowRegistry.workflowIds());
    }

    /**
     * 将 AgentScope 能力条目转换为稳定的 JSON 响应结构。
     *
     * @param capability 能力条目
     * @return JSON 友好的 Map
     */
    private Map<String, Object> capabilityPayload(AgentScopeOfficialCapability capability) {
        return Map.of(
                "name", capability.name(),
                "officialConcept", capability.officialConcept(),
                "status", capability.status().name(),
                "currentAnchor", capability.currentAnchor(),
                "expansionNote", capability.expansionNote());
    }
}
