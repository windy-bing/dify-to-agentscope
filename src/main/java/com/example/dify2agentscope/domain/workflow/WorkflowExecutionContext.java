package com.example.dify2agentscope.domain.workflow;

import com.example.dify2agentscope.domain.dify.VariableSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 单次工作流执行的运行时上下文。
 * <p>包含请求、对话状态、节点输出、执行路径和链路 traceId。
 * 每个 {@link WorkflowExecutor#execute} 调用创建一个新实例，不与其它请求共享。
 */
public class WorkflowExecutionContext {

    private final String workflowId;
    private final String traceId;
    private final WorkflowPlan plan;
    private final WorkflowRequest request;
    private final Map<String, Object> sys = new LinkedHashMap<>();
    private final Map<String, Object> startInputs = new LinkedHashMap<>();
    private final Map<String, Object> conversation = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();
    private final List<String> executionPath = new ArrayList<>();

    /**
     * 创建单次工作流执行上下文。
     *
     * @param workflowId workflow ID
     * @param plan       当前执行的工作流计划
     * @param request    当前请求
     */
    public WorkflowExecutionContext(String workflowId, WorkflowPlan plan, WorkflowRequest request) {
        this.workflowId = workflowId;
        this.traceId = request.traceId() == null || request.traceId().isBlank()
                ? UUID.randomUUID().toString()
                : request.traceId();
        this.plan = plan;
        this.request = request;
        sys.put("query", request.query());
        startInputs.put("x_dify_chat_id", request.xDifyChatId());
        startInputs.put("x_menu_id", request.xMenuId());
        for (VariableSpec variable : plan.variables()) {
            conversation.put(variable.name(), copyDefault(variable.defaultValue()));
        }
        conversation.putAll(request.conversation());
    }

    /** 当前工作流 ID */
    public String workflowId() {
        return workflowId;
    }

    /** 链路追踪 ID，未传入时自动生成 UUID */
    public String traceId() {
        return traceId;
    }

    /** Dify 工作流计划 */
    public WorkflowPlan plan() {
        return plan;
    }

    /** 本次执行的请求 */
    public WorkflowRequest request() {
        return request;
    }

    /** 对话变量上下文，可在 assigner 节点中修改 */
    public Map<String, Object> conversation() {
        return conversation;
    }

    /** 已执行节点的输出，key=nodeId, value=输出 Map */
    public Map<String, Map<String, Object>> nodeOutputs() {
        return nodeOutputs;
    }

    /** 执行路径（按序经过的节点 ID） */
    public List<String> executionPath() {
        return executionPath;
    }

    /** 记录节点输出到 nodeOutputs */
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, new LinkedHashMap<>(output));
    }

    /**
     * 按 Dify 变量选择器解析值。
     * <p>支持三种 scope：
     * <ul>
     *   <li>{@code sys} — 系统变量（query）</li>
     *   <li>{@code conversation} — 对话变量</li>
     *   <li>{@code <nodeId>} — 节点输出</li>
     * </ul>
     */
    public Object resolve(List<?> selector) {
        if (selector == null || selector.isEmpty()) {
            return null;
        }
        String scope = String.valueOf(selector.get(0));
        String key = selector.size() > 1 ? String.valueOf(selector.get(1)) : "";
        if ("sys".equals(scope)) {
            return sys.get(key);
        }
        if ("conversation".equals(scope)) {
            return conversation.get(key);
        }
        if (startInputs.containsKey(key)) {
            return startInputs.get(key);
        }
        Map<String, Object> node = nodeOutputs.get(scope);
        return node == null ? null : node.get(key);
    }

    /** 深度拷贝默认值（list/map 防引用泄漏） */
    private Object copyDefault(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        return value;
    }
}
