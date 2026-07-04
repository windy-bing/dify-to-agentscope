package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.dify.VariableSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 面向业务代码的 AgentScope code-first workflow 构建器。
 * <p>该类用于替代 Dify DSL/YAML：研发可以像官方 AgentScope 示例一样，在 Java 中逐步声明变量、
 * 记忆、知识检索、Agent、Tool、Answer 等步骤。构建结果仍会复用本项目现有运行时能力。</p>
 */
public class CodeFirstWorkflowBuilder {

    private final String workflowId;
    private final String name;
    private final List<VariableSpec> variables = new ArrayList<>();
    private final List<DifyNode> nodes = new ArrayList<>();
    private final List<DifyEdge> edges = new ArrayList<>();
    private final List<AgentSpec> agents = new ArrayList<>();
    private String mode = "agentscope-workflow";
    private String lastNodeId;

    /**
     * 创建 code-first workflow 构建器。
     *
     * @param workflowId workflow 注册 ID
     * @param name       workflow 名称
     */
    private CodeFirstWorkflowBuilder(String workflowId, String name) {
        this(workflowId, name, true);
    }

    /**
     * 创建 code-first workflow 构建器。
     *
     * @param workflowId workflow 注册 ID
     * @param name       workflow 名称
     * @param createStart 是否创建 DAG start 节点
     */
    private CodeFirstWorkflowBuilder(String workflowId, String name, boolean createStart) {
        this.workflowId = requireText(workflowId, "workflowId");
        this.name = requireText(name, "name");
        if (createStart) {
            node("start", "start", "Start", Map.of());
            this.lastNodeId = "start";
        }
    }

    /**
     * 创建 DAG 兼容模式 workflow。
     *
     * @param workflowId workflow 注册 ID
     * @param name       workflow 名称
     * @return code-first 构建器
     */
    public static CodeFirstWorkflowBuilder dag(String workflowId, String name) {
        return new CodeFirstWorkflowBuilder(workflowId, name);
    }

    /**
     * 创建官方风格 AgentScope ReAct 自循环 workflow。
     * <p>这是从零手写 workflow 的推荐入口：构建结果不生成 start/answer/edge 等 DAG 兼容结构，
     * 运行时直接进入官方 {@code ReActAgent} 调用链。</p>
     *
     * @param workflowId workflow 注册 ID
     * @param name       workflow 名称
     * @return code-first 构建器
     */
    public static CodeFirstWorkflowBuilder official(String workflowId, String name) {
        CodeFirstWorkflowBuilder builder = new CodeFirstWorkflowBuilder(workflowId, name, false);
        builder.mode = "agentscope-react";
        return builder;
    }

    /**
     * 创建 AgentScope ReAct 自循环 workflow。
     * <p>该模式不依赖外部 DAG 边，执行时直接调用第一个 Agent。</p>
     *
     * @param workflowId workflow 注册 ID
     * @param name       workflow 名称
     * @return code-first 构建器
     */
    public static CodeFirstWorkflowBuilder react(String workflowId, String name) {
        return official(workflowId, name);
    }

    /**
     * 添加会话变量。
     *
     * @param name         变量名
     * @param type         变量类型
     * @param defaultValue 默认值
     * @param description  变量说明
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder variable(String name, String type, Object defaultValue, String description) {
        variables.add(new VariableSpec(
                requireText(name, "variable name"),
                blankToDefault(type, "string"),
                description == null ? "" : description,
                defaultValue));
        return this;
    }

    /**
     * 添加 Memos 长期记忆检索节点。
     *
     * @param id    节点 ID
     * @param title 节点标题
     * @param limit 最大检索条数
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder memoRetrieval(String id, String title, int limit) {
        nodeAndConnect(id, "memo-retrieval", title, Map.of("limit", limit));
        return this;
    }

    /**
     * 添加知识库检索节点。
     *
     * @param id       节点 ID
     * @param title    节点标题
     * @param settings 知识库节点配置，透传给 {@code KnowledgeRetriever}
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder knowledgeRetrieval(String id, String title, Map<String, Object> settings) {
        nodeAndConnect(id, "knowledge-retrieval", title, settings == null ? Map.of() : settings);
        return this;
    }

    /**
     * 添加内置 code 节点。
     * <p>当前运行时支持 identifier 提取和当前时间两类内置输出。</p>
     *
     * @param id      节点 ID
     * @param title   节点标题
     * @param outputs 输出字段定义
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder code(String id, String title, Map<String, Object> outputs) {
        nodeAndConnect(id, "code", title, Map.of("outputs", outputs == null ? Map.of() : outputs));
        return this;
    }

    /**
     * 添加会话变量赋值节点。
     *
     * @param id       节点 ID
     * @param title    节点标题
     * @param variable conversation 变量名
     * @param value    常量值
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder assignConstant(String id, String title, String variable, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("variable_selector", List.of("conversation", requireText(variable, "variable")));
        item.put("input_type", "constant");
        item.put("value", value);
        item.put("operation", "set");
        nodeAndConnect(id, "assigner", title, Map.of("items", List.of(item)));
        return this;
    }

    /**
     * 添加 Agent 节点。
     *
     * @param id          节点 ID
     * @param title       Agent 标题
     * @param instruction Agent 系统指令
     * @param model       模型标识
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder agent(String id, String title, String instruction, String model) {
        return agent(id, title, instruction, model, agent -> {});
    }

    /**
     * 添加 Agent 节点并使用嵌套 builder 声明工具、推理参数和节点超时。
     *
     * @param id          节点 ID
     * @param title       Agent 标题
     * @param instruction Agent 系统指令
     * @param model       模型标识
     * @param customizer  Agent 节点配置回调
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder agent(
            String id,
            String title,
            String instruction,
            String model,
            Consumer<AgentNode> customizer) {
        AgentNode agentNode = new AgentNode(id, title, instruction, model);
        customizer.accept(agentNode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instruction", agentNode.instruction);
        data.put("model", agentNode.model);
        data.put("tools", List.copyOf(agentNode.tools));
        data.putAll(agentNode.settings);
        nodeAndConnect(agentNode.id, "agent", agentNode.title, data);
        agents.add(new AgentSpec(
                agentNode.id,
                agentNode.title,
                agentNode.instruction,
                agentNode.model,
                Map.copyOf(agentNode.completionParams),
                List.copyOf(agentNode.tools)));
        return this;
    }

    /**
     * 添加最终 answer 节点。
     *
     * @param template answer 模板，例如 {@code {{#assistant.text#}}}
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder answer(String template) {
        nodeAndConnect("answer", "answer", "Answer", Map.of("answer", template == null ? "" : template));
        return this;
    }

    /**
     * 添加原始节点。
     * <p>用于接入当前构建器尚未封装的新节点类型，仍会复用现有执行器和观测能力。</p>
     *
     * @param id       节点 ID
     * @param type     节点类型
     * @param title    节点标题
     * @param settings 节点配置
     * @return 当前构建器
     */
    public CodeFirstWorkflowBuilder rawNode(String id, String type, String title, Map<String, Object> settings) {
        nodeAndConnect(id, type, title, settings == null ? Map.of() : settings);
        return this;
    }

    /**
     * 构建 code-first workflow 定义。
     *
     * @return 可注册到运行时的 workflow 定义
     */
    public CodeFirstWorkflowDefinition build() {
        boolean hasAnswer = nodes.stream().anyMatch(node -> "answer".equals(node.type()));
        if (!hasAnswer && !isReactSelfLoopMode()) {
            answer("{{#" + lastAgentNodeId() + ".text#}}");
        }
        if (isReactSelfLoopMode() && agents.isEmpty()) {
            throw new IllegalStateException("React code-first workflow must contain at least one agent");
        }
        WorkflowPlan plan = new WorkflowPlan(
                name,
                mode,
                "",
                List.copyOf(variables),
                List.copyOf(nodes),
                List.copyOf(edges),
                List.copyOf(agents));
        return new CodeFirstWorkflowDefinition(workflowId, plan);
    }

    /**
     * 判断当前是否为 ReAct 自循环模式。
     *
     * @return true 表示 ReAct 自循环模式
     */
    private boolean isReactSelfLoopMode() {
        return "agentscope-react".equalsIgnoreCase(mode) || "react-self-loop".equalsIgnoreCase(mode);
    }

    /**
     * 添加节点并连接上一个节点。
     *
     * @param id       节点 ID
     * @param type     节点类型
     * @param title    节点标题
     * @param settings 节点配置
     */
    private void nodeAndConnect(String id, String type, String title, Map<String, Object> settings) {
        node(id, type, title, settings);
        connect(lastNodeId, id, "source");
        lastNodeId = id;
    }

    /**
     * 添加节点。
     *
     * @param id       节点 ID
     * @param type     节点类型
     * @param title    节点标题
     * @param settings 节点配置
     */
    private void node(String id, String type, String title, Map<String, Object> settings) {
        String nodeId = requireText(id, "node id");
        String nodeType = requireText(type, "node type");
        Map<String, Object> data = new LinkedHashMap<>(settings);
        data.put("type", nodeType);
        data.put("title", blankToDefault(title, nodeId));
        nodes.add(new DifyNode(nodeId, nodeType, blankToDefault(title, nodeId), data));
    }

    /**
     * 添加有向边。
     *
     * @param source 源节点 ID
     * @param target 目标节点 ID
     * @param handle 输出句柄
     */
    private void connect(String source, String target, String handle) {
        if (source == null || source.isBlank()) {
            return;
        }
        edges.add(new DifyEdge(source + "-" + handle + "-" + target, source, handle, target));
    }

    /**
     * 获取最后一个 Agent 节点 ID。
     *
     * @return Agent 节点 ID
     */
    private String lastAgentNodeId() {
        if (agents.isEmpty()) {
            throw new IllegalStateException("Workflow must contain at least one agent before adding answer");
        }
        return agents.get(agents.size() - 1).nodeId();
    }

    /**
     * 校验并返回非空字符串。
     *
     * @param value 原始值
     * @param name  字段名
     * @return 非空字符串
     */
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /**
     * 空白字符串回退默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 非空字符串
     */
    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * Agent 节点的 code-first 嵌套配置对象。
     */
    public static class AgentNode {

        private final String id;
        private final String title;
        private final String instruction;
        private final String model;
        private final List<ToolSpec> tools = new ArrayList<>();
        private final Map<String, Object> completionParams = new LinkedHashMap<>();
        private final Map<String, Object> settings = new LinkedHashMap<>();

        /**
         * 创建 Agent 节点配置对象。
         *
         * @param id          节点 ID
         * @param title       Agent 标题
         * @param instruction Agent 指令
         * @param model       模型标识
         */
        private AgentNode(String id, String title, String instruction, String model) {
            this.id = requireText(id, "agent id");
            this.title = blankToDefault(title, this.id);
            this.instruction = instruction == null ? "" : instruction;
            this.model = blankToDefault(model, "dashscope:qwen-plus");
        }

        /**
         * 添加工具声明。
         *
         * @param name        工具名称
         * @param label       工具展示名
         * @param description 工具说明
         * @param provider    工具提供方
         * @param parameters  工具参数 schema 或静态参数
         * @return 当前 Agent 节点配置
         */
        public AgentNode tool(
                String name,
                String label,
                String description,
                String provider,
                Object parameters) {
            tools.add(new ToolSpec(
                    requireText(name, "tool name"),
                    blankToDefault(label, name),
                    description == null ? "" : description,
                    blankToDefault(provider, "agentscope"),
                    parameters == null ? Map.of() : parameters));
            return this;
        }

        /**
         * 设置 Agent completion 参数。
         *
         * @param key   参数名
         * @param value 参数值
         * @return 当前 Agent 节点配置
         */
        public AgentNode completionParam(String key, Object value) {
            completionParams.put(requireText(key, "completion param key"), value);
            return this;
        }

        /**
         * 设置节点级超时。
         *
         * @param timeout 节点超时时长
         * @return 当前 Agent 节点配置
         */
        public AgentNode timeout(Duration timeout) {
            if (timeout != null) {
                settings.put("timeout-ms", timeout.toMillis());
            }
            return this;
        }

        /**
         * 为官方 ReAct 调用准备 Memos 长期记忆上下文。
         * <p>该配置不会创建 DAG 节点；运行时会在调用 Agent 前检索记忆并注入 conversation。</p>
         *
         * @param limit 最大检索条数
         * @return 当前 Agent 节点配置
         */
        public AgentNode memos(int limit) {
            settings.put("memo-limit", limit);
            return this;
        }

        /**
         * 为官方 ReAct 调用准备知识库上下文。
         * <p>该配置不会创建 DAG 节点；运行时会在调用 Agent 前通过 {@code KnowledgeRetriever} 检索并注入 conversation。</p>
         *
         * @param settings 知识库检索配置
         * @return 当前 Agent 节点配置
         */
        public AgentNode knowledge(Map<String, Object> settings) {
            this.settings.put("knowledge", settings == null ? Map.of() : Map.copyOf(settings));
            return this;
        }

        /**
         * 设置原始节点配置。
         *
         * @param key   配置名
         * @param value 配置值
         * @return 当前 Agent 节点配置
         */
        public AgentNode setting(String key, Object value) {
            settings.put(requireText(key, "setting key"), value);
            return this;
        }
    }
}
