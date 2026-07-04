# AgentScope 官方能力边界

本项目不会把 Dify/workflow 迁移层包装成 AgentScope Java 官方实现。当前策略是：

1. 官方能力优先：Agent、Message、Event、Model、Toolkit、Tool、RuntimeContext、StateStore 等能用官方 API 的必须走官方 API。
2. 只有官方没有覆盖 Dify 迁移语义时才保留项目适配层，例如 Dify DSL 解析、Dify 节点图执行、节点超时策略。
3. 官方已有但当前缺少输入配置模型的能力，先保留最小扩展点，后续按官方 API 接入，不重新造一套。

## 当前能力表

| 能力 | 官方概念 | 当前状态 | 当前锚点 |
| --- | --- | --- | --- |
| Code-first Authoring | `ReActAgent.builder / Toolkit` 风格 | 项目自定义入口，官方风格优先 | `CodeFirstWorkflowBuilder / CodeFirstWorkflowRuntime` |
| Agent | `Agent / ReActAgent` | 已用官方 API | `AgentScopeAgentFactory` |
| Message | `Msg / UserMessage / AssistantMessage` | 已用官方 API | `AgentScopeStreamingAgentInvoker` |
| Event Streaming | `AgentEvent / TextBlockDeltaEvent` | 已用官方 API | `AgentScopeStreamingAgentInvoker` |
| Model | `ModelRegistry / Model` | 已用官方 API | `AgentScopeAgentFactory#toAgentScopeModel` |
| Toolkit | `Toolkit` | 已用官方 API | `AgentScopeAgentFactory` |
| Tool | `@Tool / ToolBase / AgentTool` | 最小适配 | `DifyMcpToolStub` |
| MCP | `McpClientBuilder / Toolkit.registerMcpClient` | 最小适配 | `ToolGateway` |
| Permission/HITL | `PermissionDecision / PermissionContext` | 最小适配 | `PermissionPolicy` |
| State Store | `AgentState / AgentStateStore` | 已用官方 API | `WorkflowRuntimeConfig#agentScopeAgentStateStore` |
| Runtime Context | `RuntimeContext` | 已用官方 API | `AgentScopeStreamingAgentInvoker` |
| Long-term Memory | `Long-term Memory` | 最小适配 | `MemoStore` |
| Structured Output | `Structured Output` | 扩展点 | `AgentScopeOfficialCapability` |
| Middleware | `Middleware` | 扩展点 | `AgentScopeOfficialCapability` |
| Skill | `Skill` | 扩展点 | `NacosResourceCatalog` |
| Plan | `PlanNotebook` | 扩展点 | `AgentScopeOfficialCapability` |
| Harness / Multi Agent | `HarnessAgent / Subagent / Task tools` | 扩展点 | `WorkflowExecutor#isHarnessMode` |
| Project Workflow DAG | 非官方核心能力 | 项目自定义 | `AgentScopeWorkflowPlan` |

代码里的单一事实源是 `AgentScopeOfficialCapability`，HTTP 诊断入口：

```http
GET /api/v1/workflows/agentscope/capabilities
```

## 后续扩展原则

- 官方 Agent 能力继续从 `AgentScopeAgentFactory` 接入，不在 `WorkflowExecutor` 里直接 new 官方对象。
- 没有 Dify/YAML 时优先走 `CodeFirstWorkflowBuilder.official(...)`，用 Java 代码声明官方 ReAct Agent、Tool、Memory 和 Knowledge，再注册到现有 runtime；`dag(...)` 只作为迁移兼容入口，HTTP 模板只作为快速创建最小 Agent 的 API。
- 官方消息/事件能力已经使用 `UserMessage` 和 `TextBlockDeltaEvent`；下一步是从“字符串 prompt”升级为完整 `Msg` 块映射，并将 `AgentEvent` 透传到 SSE。
- 官方状态能力已经接入共享 `AgentStateStore` Bean；项目 `SessionMemoryStore` 只保留为 Dify conversation 兼容层，不作为 ReActAgent 状态源。
- 官方工具能力优先通过 `Toolkit`、`ToolBase`、`@Tool`、MCP client 扩展；`ToolGateway` 继续作为 Dify/Higress 兼容边界。
- 项目 workflow DAG 只作为迁移兼容模式存在；入口解析后先归一到 `AgentScopeWorkflowPlan`，后续可按 `REACT_SELF_LOOP` 或 `HARNESS_MULTI_AGENT` 改造成官方自循环能力。`HARNESS_MULTI_AGENT` 在官方 Harness 接入前会显式失败，不降级成单 Agent ReAct。

## 迁移策略

1. `Dify DSL/YAML -> WorkflowPlan` 只作为入口兼容。
2. `WorkflowPlan -> AgentScopeWorkflowPlan` 是运行时规范化边界。
3. `AgentScopeExecutionMode.DAG` 用于转换后立即运行。
4. `AgentScopeExecutionMode.REACT_SELF_LOOP` 用于单 Agent 自循环，优先依赖官方 `ReActAgent`。
5. `AgentScopeExecutionMode.HARNESS_MULTI_AGENT` 用于后续多 Agent/子 Agent，优先依赖官方 Harness 能力。
