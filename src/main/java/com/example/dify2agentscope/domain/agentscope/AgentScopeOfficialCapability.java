package com.example.dify2agentscope.domain.agentscope;

/**
 * AgentScope Java 官方能力清单的最小镜像。
 * <p>这里不复制官方实现，只记录当前项目应保留的能力槽位、官方概念名称、当前落地状态和后续扩展入口。
 * 这样新增能力时可以按官方边界扩展，而不是继续把所有逻辑堆进 workflow executor。</p>
 */
public enum AgentScopeOfficialCapability {

    /** Code-first 编写入口：按官方 builder 风格手写 workflow，再注册到项目运行时。 */
    CODE_FIRST_AUTHORING(
            "ReActAgent.builder / Toolkit authoring style",
            AgentScopeCapabilityStatus.PROJECT_DEFINED,
            "application.workflow.CodeFirstWorkflowBuilder",
            "没有 Dify/YAML 时优先用 Java code-first 声明官方 ReAct Agent、Tool、Memory 和 Knowledge，再交给 CodeFirstWorkflowRuntime 注册。"),

    /** 官方 Agent 抽象：Agent/ReActAgent，当前项目已用 ReActAgent 构建 agent 节点。 */
    AGENT(
            "Agent / ReActAgent",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeAgentFactory",
            "继续扩展 HarnessAgent 或多 Agent 时应在 Agent 工厂层完成。"),

    /** 官方消息抽象：Msg/UserMessage/AssistantMessage 等，当前项目用 UserMessage 发送输入。 */
    MESSAGE(
            "Msg / UserMessage / AssistantMessage",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeStreamingAgentInvoker",
            "当前已用 UserMessage；后续应把 WorkflowRequest/Response 完整映射为 Msg/AssistantMessage。"),

    /** 官方事件抽象：AgentEvent 和流式 delta，当前项目收集 TextBlockDeltaEvent。 */
    EVENT_STREAMING(
            "AgentEvent / TextBlockDeltaEvent",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeStreamingAgentInvoker",
            "SSE 应逐步透传 AgentEvent，而不是等待完整文本后一次返回。"),

    /** 官方模型抽象：ModelRegistry 字符串模型标识或显式 Model builder。 */
    MODEL(
            "ModelRegistry / Model",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeAgentFactory#toAgentScopeModel",
            "当前使用官方 ModelRegistry 字符串模型标识；后续应支持模型参数、fallback model、retry 和 provider 专属配置。"),

    /** 官方工具容器：Toolkit，当前项目把 Dify/MCP 工具注册为 ToolBase stub。 */
    TOOLKIT(
            "Toolkit",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeAgentFactory",
            "后续扩展 tool group、skill tool、内置 TodoTools 时保持从 Toolkit 入口接入。"),

    /** 官方工具抽象：@Tool/ToolBase/AgentTool，当前项目以 DifyMcpToolStub 做最小桥接。 */
    TOOL(
            "@Tool / ToolBase / AgentTool",
            AgentScopeCapabilityStatus.PROJECT_MINIMAL_ADAPTER,
            "infrastructure.mcp.DifyMcpToolStub",
            "后续可增加注解工具、ToolBase 工具和 external execution 工具。"),

    /** 官方 MCP 能力：Toolkit.registerMcpClient，当前项目先经 Higress MCP 网关转发。 */
    MCP(
            "McpClientBuilder / Toolkit.registerMcpClient",
            AgentScopeCapabilityStatus.PROJECT_MINIMAL_ADAPTER,
            "domain.integration.ToolGateway",
            "如需直连 MCP Server，应新增 AgentScope MCP 客户端适配器，不替换 ToolGateway 抽象。"),

    /** 官方权限/HITL：PermissionDecision/PermissionContext，当前项目已有简化 PermissionPolicy。 */
    PERMISSION_HITL(
            "PermissionDecision / PermissionContext / HITL events",
            AgentScopeCapabilityStatus.PROJECT_MINIMAL_ADAPTER,
            "domain.security.PermissionPolicy",
            "后续敏感工具审批应接入官方 permission context 和 RequireUserConfirmEvent。"),

    /** 官方状态持久化：AgentStateStore，当前项目的 session memory 是项目级简化记忆。 */
    STATE_STORE(
            "AgentState / AgentStateStore",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "config.WorkflowRuntimeConfig#agentScopeAgentStateStore",
            "真实 ReActAgent 已挂共享官方 InMemoryAgentStateStore；生产化应切 JsonFile/Redis/DB 实现。"),

    /** 官方 RuntimeContext：已传入 userId/sessionId，并把项目字段放入 attributes。 */
    RUNTIME_CONTEXT(
            "RuntimeContext",
            AgentScopeCapabilityStatus.OFFICIAL_API_USED,
            "infrastructure.agentscope.AgentScopeStreamingAgentInvoker",
            "已把 userId + xDifyChatId 映射为官方 RuntimeContext；workflowId/traceId/menuId 放入调用扩展属性。"),

    /** 官方长期记忆/语义检索能力，当前 Memos 是外部长期记忆适配，不等于官方 long-term memory。 */
    LONG_TERM_MEMORY(
            "Long-term Memory",
            AgentScopeCapabilityStatus.PROJECT_MINIMAL_ADAPTER,
            "domain.memory.MemoStore",
            "Memos 继续作为事实库；若需要官方语义记忆，应新增独立 Memory 适配器。"),

    /** 官方结构化输出能力，当前项目还未接入。 */
    STRUCTURED_OUTPUT(
            "Structured Output",
            AgentScopeCapabilityStatus.EXTENSION_POINT_ONLY,
            "domain.agentscope.AgentScopeOfficialCapability",
            "官方支持 ReActAgent structured output overload；当前节点 schema 尚未建模，因此只保留扩展点。"),

    /** 官方中间件能力，当前项目还未接入。 */
    MIDDLEWARE(
            "Middleware",
            AgentScopeCapabilityStatus.EXTENSION_POINT_ONLY,
            "domain.agentscope.AgentScopeOfficialCapability",
            "官方支持 Middleware；当前没有节点级 middleware 配置模型，后续日志、prompt 注入、模型改写应优先落在 middleware。"),

    /** 官方 Skill 能力，当前项目通过 Nacos/资源目录保留 prompt/skill 配置入口。 */
    SKILL(
            "Skill",
            AgentScopeCapabilityStatus.EXTENSION_POINT_ONLY,
            "domain.nacos.NacosResourceCatalog",
            "后续可把 Nacos skill 映射为 Toolkit skill 或 skill viewer tool。"),

    /** 官方 PlanNotebook/计划能力，当前项目还未接入。 */
    PLAN(
            "PlanNotebook",
            AgentScopeCapabilityStatus.EXTENSION_POINT_ONLY,
            "domain.agentscope.AgentScopeOfficialCapability",
            "官方 PlanNotebook 是 Agent 内部任务计划；当前 Dify DAG 是外部编排层，两者语义不同，后续可作为 agent tool/middleware 接入。"),

    /** 官方 HarnessAgent/多 Agent/子任务能力，当前项目只暴露 workflow 和 A2A。 */
    HARNESS_MULTI_AGENT(
            "HarnessAgent / Subagent / Task tools",
            AgentScopeCapabilityStatus.EXTENSION_POINT_ONLY,
            "domain.a2a",
            "后续多 agent 会话、子 agent spawn/send/history 可参考官方 harness。"),

    /** 本项目自定义 workflow DAG，不是 AgentScope Java 官方核心 workflow 类型。 */
    PROJECT_WORKFLOW_DAG(
            "Project Workflow DAG",
            AgentScopeCapabilityStatus.PROJECT_DEFINED,
            "domain.agentscope.AgentScopeWorkflowPlan",
            "DAG 只作为迁移兼容执行模式保留；运行时模型已归一到 AgentScopeWorkflowPlan。");

    private final String officialConcept;
    private final AgentScopeCapabilityStatus status;
    private final String currentAnchor;
    private final String expansionNote;

    /**
     * 创建官方能力条目。
     *
     * @param officialConcept AgentScope 官方概念名
     * @param status          当前项目落地状态
     * @param currentAnchor   当前项目里的最小实现或扩展入口
     * @param expansionNote   后续扩展说明
     */
    AgentScopeOfficialCapability(
            String officialConcept,
            AgentScopeCapabilityStatus status,
            String currentAnchor,
            String expansionNote) {
        this.officialConcept = officialConcept;
        this.status = status;
        this.currentAnchor = currentAnchor;
        this.expansionNote = expansionNote;
    }

    /**
     * 获取官方概念名称。
     *
     * @return 官方概念名称
     */
    public String officialConcept() {
        return officialConcept;
    }

    /**
     * 获取当前落地状态。
     *
     * @return 能力状态
     */
    public AgentScopeCapabilityStatus status() {
        return status;
    }

    /**
     * 获取当前项目中的最小实现或扩展入口。
     *
     * @return 当前实现锚点
     */
    public String currentAnchor() {
        return currentAnchor;
    }

    /**
     * 获取后续扩展建议。
     *
     * @return 扩展说明
     */
    public String expansionNote() {
        return expansionNote;
    }
}
