package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.dify.VariableSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 底层兼容工作流构建器。
 * <p>该类只负责生成兼容 {@link WorkflowPlan}，不包含 workflowId、运行时注册和完整中间件声明。
 * 新业务代码需要从零手写 workflow 时，应优先使用 {@link CodeFirstWorkflowBuilder}
 * 和 {@link CodeFirstWorkflowRuntime}。</p>
 */
public class WorkflowBuilder {

    private final String name;
    private final List<VariableSpec> variables = new ArrayList<>();
    private final List<DifyNode> nodes = new ArrayList<>();
    private final List<DifyEdge> edges = new ArrayList<>();
    private final List<AgentSpec> agents = new ArrayList<>();
    private String mode = "agentscope-workflow";
    private String lastNodeId;

    /**
     * 初始化构建器并自动创建 start 节点。
     *
     * @param name 工作流名称
     */
    private WorkflowBuilder(String name) {
        this.name = name;
        node("start", "start", "Start", Map.of());
        lastNodeId = "start";
    }

    /**
     * 创建新的工作流构建器。
     *
     * @param name 工作流名称
     * @return 工作流构建器
     */
    public static WorkflowBuilder create(String name) {
        return new WorkflowBuilder(name);
    }

    /**
     * 创建 AgentScope ReAct 自循环 workflow 构建器。
     * <p>该模式默认不依赖 DAG 边，运行时直接调用第一个 Agent，由官方 ReActAgent 自行完成 reasoning/acting。</p>
     *
     * @param name workflow 名称
     * @return workflow 构建器
     */
    public static WorkflowBuilder createReactSelfLoop(String name) {
        WorkflowBuilder builder = new WorkflowBuilder(name);
        builder.mode = "agentscope-react";
        return builder;
    }

    /**
     * 添加对话变量定义。
     *
     * @param name         变量名
     * @param type         变量类型
     * @param defaultValue 默认值
     * @param description  变量说明
     * @return 当前构建器
     */
    public WorkflowBuilder variable(String name, String type, Object defaultValue, String description) {
        variables.add(new VariableSpec(name, type, description == null ? "" : description, defaultValue));
        return this;
    }

    /**
     * 添加 Agent 节点，不配置工具。
     *
     * @param id          节点 ID
     * @param title       节点标题
     * @param instruction Agent 指令
     * @param model       模型标识
     * @return 当前构建器
     */
    public WorkflowBuilder agent(String id, String title, String instruction, String model) {
        return agent(id, title, instruction, model, List.of());
    }

    /**
     * 添加 Agent 节点并配置工具列表。
     *
     * @param id          节点 ID
     * @param title       节点标题
     * @param instruction Agent 指令
     * @param model       模型标识
     * @param tools       工具定义列表
     * @return 当前构建器
     */
    public WorkflowBuilder agent(String id, String title, String instruction, String model, List<ToolSpec> tools) {
        node(id, "agent", title, Map.of(
                "instruction", instruction == null ? "" : instruction,
                "model", model == null || model.isBlank() ? "dashscope:qwen-plus" : model,
                "tools", tools == null ? List.of() : tools));
        agents.add(new AgentSpec(
                id,
                title == null || title.isBlank() ? id : title,
                instruction == null ? "" : instruction,
                model == null || model.isBlank() ? "dashscope:qwen-plus" : model,
                Map.of(),
                tools == null ? List.of() : List.copyOf(tools)));
        connect(lastNodeId, id, "source");
        lastNodeId = id;
        return this;
    }

    /**
     * 添加 Memos 记忆检索节点。
     *
     * @param id    节点 ID
     * @param title 节点标题
     * @param limit 最大返回条数
     * @return 当前构建器
     */
    public WorkflowBuilder memoRetrieval(String id, String title, int limit) {
        node(id, "memo-retrieval", title, Map.of("limit", limit));
        connect(lastNodeId, id, "source");
        lastNodeId = id;
        return this;
    }

    /**
     * 添加 answer 节点作为工作流最终回复。
     *
     * @param template answer 模板
     * @return 当前构建器
     */
    public WorkflowBuilder answer(String template) {
        node("answer", "answer", "Answer", Map.of("answer", template));
        connect(lastNodeId, "answer", "source");
        lastNodeId = "answer";
        return this;
    }

    /**
     * 构建不可变工作流计划。
     * <p>DAG 模式下如果调用方没有显式添加 answer 节点，则默认使用最后一个 Agent 的 text 输出。
     * ReAct 自循环模式由官方 Agent 自行产出最终回答，不再自动补充 DAG answer 节点。</p>
     *
     * @return 工作流计划
     */
    public WorkflowPlan build() {
        boolean hasAnswer = nodes.stream().anyMatch(node -> "answer".equals(node.type()));
        if (!hasAnswer && !isReactSelfLoopMode()) {
            answer("{{#" + lastAgentNodeId() + ".text#}}");
        }
        if (isReactSelfLoopMode() && agents.isEmpty()) {
            throw new IllegalStateException("React self-loop workflow must contain at least one agent");
        }
        return new WorkflowPlan(
                name,
                mode,
                "",
                List.copyOf(variables),
                List.copyOf(nodes),
                List.copyOf(edges),
                List.copyOf(agents));
    }

    /**
     * 判断当前构建器是否处于 AgentScope ReAct 自循环模式。
     *
     * @return true 表示不需要自动生成 DAG answer 节点
     */
    private boolean isReactSelfLoopMode() {
        return "agentscope-react".equalsIgnoreCase(mode) || "react-self-loop".equalsIgnoreCase(mode);
    }

    /**
     * 添加内部节点并补齐通用 data 字段。
     *
     * @param id     节点 ID
     * @param type   节点类型
     * @param title  节点标题
     * @param values 节点配置
     */
    private void node(String id, String type, String title, Map<String, Object> values) {
        Map<String, Object> data = new LinkedHashMap<>(values);
        data.put("type", type);
        data.put("title", title == null || title.isBlank() ? id : title);
        nodes.add(new DifyNode(id, type, title == null || title.isBlank() ? id : title, data));
    }

    /**
     * 连接两个节点。
     *
     * @param source 源节点 ID
     * @param target 目标节点 ID
     * @param handle 源输出句柄
     */
    private void connect(String source, String target, String handle) {
        edges.add(new DifyEdge(source + "-" + handle + "-" + target, source, handle, target));
    }

    /**
     * 获取最后一个 Agent 节点 ID。
     *
     * @return Agent 节点 ID
     */
    private String lastAgentNodeId() {
        if (agents.isEmpty()) {
            throw new IllegalStateException("Workflow must contain at least one agent before adding a default answer");
        }
        return agents.get(agents.size() - 1).nodeId();
    }
}
