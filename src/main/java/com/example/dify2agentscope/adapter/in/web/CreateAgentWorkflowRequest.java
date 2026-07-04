package com.example.dify2agentscope.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从零创建最小 AgentScope workflow 的 HTTP 请求体。
 * <p>executionMode 默认为 dag，适合迁移后立即运行；传 react/agentscope-react 时创建 ReAct 自循环计划。</p>
 */
public class CreateAgentWorkflowRequest {

    @NotBlank
    private String workflowId;
    @NotBlank
    private String name;
    private String agentId = "assistant";
    private String agentTitle = "Assistant";
    private String instruction = "Answer the user concisely.";
    private String model = "dashscope:qwen-plus";
    private String executionMode = "dag";
    private boolean memoRetrieval;
    private Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * 获取新 workflow 的注册 ID。
     *
     * @return workflow ID
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * 设置新 workflow 的注册 ID。
     *
     * @param workflowId workflow ID
     */
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * 获取 workflow 展示名称。
     *
     * @return workflow 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置 workflow 展示名称。
     *
     * @param name workflow 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取模板中 Agent 节点 ID。
     *
     * @return Agent 节点 ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 设置模板中 Agent 节点 ID。
     *
     * @param agentId Agent 节点 ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 获取 Agent 展示标题。
     *
     * @return Agent 标题
     */
    public String getAgentTitle() {
        return agentTitle;
    }

    /**
     * 设置 Agent 展示标题。
     *
     * @param agentTitle Agent 标题
     */
    public void setAgentTitle(String agentTitle) {
        this.agentTitle = agentTitle;
    }

    /**
     * 获取 Agent 系统指令。
     *
     * @return Agent 指令
     */
    public String getInstruction() {
        return instruction;
    }

    /**
     * 设置 Agent 系统指令。
     *
     * @param instruction Agent 指令
     */
    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    /**
     * 获取 Agent 使用的模型标识。
     *
     * @return 模型标识
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置 Agent 使用的模型标识。
     *
     * @param model 模型标识
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取 workflow 执行模式。
     *
     * @return dag/react/agentscope-react 等执行模式
     */
    public String getExecutionMode() {
        return executionMode;
    }

    /**
     * 设置 workflow 执行模式，空值时回落为 dag。
     *
     * @param executionMode 执行模式
     */
    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode == null || executionMode.isBlank() ? "dag" : executionMode;
    }

    /**
     * 判断是否自动插入 Memos 检索节点。
     *
     * @return true 表示创建时插入 memo-retrieval 节点
     */
    public boolean isMemoRetrieval() {
        return memoRetrieval;
    }

    /**
     * 设置是否自动插入 Memos 检索节点。
     *
     * @param memoRetrieval true 表示插入 Memos 检索节点
     */
    public void setMemoRetrieval(boolean memoRetrieval) {
        this.memoRetrieval = memoRetrieval;
    }

    /**
     * 获取 workflow 变量默认值。
     *
     * @return 变量名到默认值的映射
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 设置 workflow 变量默认值，空值时使用空 Map。
     *
     * @param variables 变量名到默认值的映射
     */
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
    }
}
