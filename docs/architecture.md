# Architecture

本工程是一个 Spring Boot 运行时，用于承载 AgentScope workflow。Dify DSL 只是导入格式，进入运行时后会先归一到 AgentScope 规范模型。

## 包结构

- `adapter/in/web`: REST、SSE、Nacos 资源查询等 HTTP 入口。
- `adapter/in/a2a`: A2A AgentCard 和 Task HTTP 入口。
- `application/workflow`: 工作流编排、请求边界、同步和流式响应。
- `application/a2a`: A2A 任务服务和 workflow 映射。
- `config`: Spring Boot 配置属性、bean 装配、启动校验。
- `cli`: 本地 DSL 转换和诊断入口。
- `domain/dify`: 入口兼容计划模型，保留 Dify DSL 和历史手写 YAML 的字段结构。
- `domain/agentscope`: AgentScope Java 官方能力清单、落地状态和扩展锚点。
- `domain/workflow`: 运行请求、响应、上下文、节点执行抽象。
- `domain/integration`: MCP 工具、知识库、模型调用等集成边界。
- `domain/security`: 请求权限、工具权限、输出清洗。
- `domain/nacos`: Nacos prompt、skill、MCP 资源目录。
- `domain/trace`: 节点级链路追踪抽象。
- `infrastructure/dify`: Dify DSL 解析和兼容生成物写出。
- `infrastructure/agentscope`: AgentScope 规范模型映射、官方 Agent 构建和调用适配。
- `infrastructure/mcp`: Higress MCP JSON-RPC 网关适配。
- `infrastructure/knowledge`: 知识库 HTTP 适配。
- `infrastructure/nacos`: Nacos prompt、skill、MCP 配置同步。
- `infrastructure/trace`: SLF4J/MDC 链路日志实现。

## 运行流程

1. 按 `workflowId` 从 `dify.agentscope.workflows` 加载 Dify DSL 或 AgentScope YAML。
2. 入口文件先解析为兼容 `WorkflowPlan`，再转换为 `AgentScopeWorkflowPlan`。
3. 可选生成审计产物到 `target/generated-workflows/{workflowId}`。
4. HTTP 或 A2A 请求进入应用层。
5. 请求边界校验必填字段、长度和工具权限。
6. `WorkflowExecutor` 按执行模式运行：`DAG` 走迁移兼容图执行，`REACT_SELF_LOOP` 直接调用官方 `ReActAgent`。
7. `HARNESS_MULTI_AGENT` 当前只保留模式入口，官方 Harness 未接入前会明确失败。
8. 每个节点进入、完成、失败均通过 `ExecutionTracer` 记录日志。
9. 知识库节点走 `KnowledgeRetriever`。
10. MCP 工具调用走 `ToolGateway`，默认实现为 Higress MCP JSON-RPC。
11. 最终输出经过 `OutputSanitizer` 后返回。

## AgentScope 官方边界

当前项目直接使用的官方 API 主要是 `ReActAgent`、`Toolkit`、`UserMessage`、`AgentEvent` 流式事件、
`RuntimeContext` 和 `AgentStateStore`。`WorkflowExecutor`、Dify DSL 转换、手写 workflow YAML、节点超时和
Memos、DAG 兼容执行和节点超时是本项目适配层，不是 AgentScope Java 官方核心实现。
完整能力清单见 `docs/agentscope-official-capabilities.md`，运行时可通过 `GET /api/v1/workflows/agentscope/capabilities` 查看。

## 生产边界

- 请求边界：`query`、`x_dify_chat_id`、`x_menu_id`、`user_id` 必填。
- 大小边界：请求和输出长度通过配置控制。
- 工具边界：agent 只能调用 DSL 节点中声明的工具。
- 参数边界：模板参数在服务端解析后再进入网关。
- 集成边界：Higress MCP、知识库 HTTP、Nacos 均通过配置注入。
- 启动边界：生产可设置 `fail-on-missing-integrations=true`，缺少关键集成直接启动失败。
- 链路边界：`traceId` 写入响应、A2A metadata 和日志 MDC。

## 关键配置

开发模式允许 stub：

```yaml
dify:
  agentscope:
    boundary:
      allow-stub-integrations: true
```

生产模式建议关闭 stub：

```yaml
dify:
  agentscope:
    fail-on-missing-integrations: true
    boundary:
      allow-stub-integrations: false
```

节点执行保护：

```yaml
dify:
  agentscope:
    execution:
      max-steps: 100
      fallback-answer: "当前系统繁忙，无法联网工作，请稍后重试！"
```

## API

```http
POST /api/v1/workflows/{workflowId}/chat
POST /api/v1/workflows/{workflowId}/stream
GET /api/v1/workflows/{workflowId}/metadata
GET /api/v1/workflows/agentscope/capabilities
GET /api/v1/workflows
GET /api/v1/nacos/resources
GET /actuator/health
```

多个 workflow 默认共用 Higress MCP、知识库 HTTP、Nacos 连接。执行隔离由 `workflowId` 完成。
