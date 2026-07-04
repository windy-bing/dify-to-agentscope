# 新人上手与运行手册

这份文档面向第一次接手项目的研发，目标是回答四类问题：

- 项目怎么跑起来、怎么判断跑通。
- Dify DSL / AgentScope YAML 如何转换和运行。
- 没有 YAML/DSL 时如何从零创建 workflow。
- 接入中间件、监控观测和排障时应该看哪里。

## 一句话理解

本项目是 AgentScope Java / Spring Boot workflow 运行时。

Dify DSL 只是导入格式；手写 YAML、HTTP 模板创建和 Java `WorkflowBuilder` 也只是入口。所有入口都会先进入兼容 `WorkflowPlan`，随后转换为 `AgentScopeWorkflowPlan`，再按执行模式运行：

- `DAG`: 迁移兼容模式，保留 Dify DAG 的节点和边，转换后能立刻运行。
- `REACT_SELF_LOOP`: 单 Agent 自循环模式，直接调用官方 `ReActAgent`。
- `HARNESS_MULTI_AGENT`: 官方 Harness/Multi-Agent 预留模式，当前未接入前会明确失败。

核心代码入口：

- 配置装配：`src/main/java/com/example/dify2agentscope/config/WorkflowRuntimeConfig.java`
- workflow 注册：`src/main/java/com/example/dify2agentscope/application/workflow/WorkflowRegistry.java`
- workflow 执行：`src/main/java/com/example/dify2agentscope/application/workflow/WorkflowExecutor.java`
- 入口归一：`src/main/java/com/example/dify2agentscope/infrastructure/agentscope/AgentScopeWorkflowPlanMapper.java`
- Agent 构建：`src/main/java/com/example/dify2agentscope/infrastructure/agentscope/AgentScopeAgentFactory.java`

## 本地启动

1. 确认 JDK 17 可用。
2. 运行测试：

```bash
mvn test
```

3. 启动服务：

```bash
mvn spring-boot:run
```

4. 验证健康检查：

```bash
curl --noproxy '*' http://127.0.0.1:8080/actuator/health
```

5. 验证默认 workflow：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/default/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"你好","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user","trace_id":"trace-demo"}'
```

默认 `build-agents=false`，因此本地会使用 Stub Agent，不依赖真实模型和 MCP。

## 配置入口

主配置文件是 `src/main/resources/application.yml`。

常用配置：

```yaml
dify:
  agentscope:
    default-workflow-id: default
    generated-output-dir: target/generated-workflows
    build-agents: false
    fail-on-missing-integrations: false
    request-timeout: 10s
    workflows:
      default:
        source-type: dify
        dsl-location: classpath:workflows/default.dsl
      manual-demo:
        source-type: agentscope
        location: classpath:workflows/manual-demo.workflow.yml
```

关键开关：

- `build-agents=false`: 使用 Stub Agent，适合本地转换、接口联调和测试。
- `build-agents=true`: 构建官方 AgentScope `ReActAgent`。
- `fail-on-missing-integrations=true`: 缺少 Higress MCP / Knowledge HTTP 等关键集成时启动失败，适合生产。
- `boundary.allow-stub-integrations=false`: 禁止 Stub 集成，防止生产误跑假实现。

## Dify DSL 转换

### 服务启动时转换

在 `application.yml` 配置：

```yaml
dify:
  agentscope:
    workflows:
      demo:
        source-type: dify
        dsl-location: classpath:workflows/default.dsl
```

启动时流程：

1. `WorkflowRuntimeConfig#parseWorkflow` 读取资源。
2. `DifyDslParser` 解析 Dify DSL 为兼容 `WorkflowPlan`。
3. `AgentScopeWorkflowPlanMapper` 转换为 `AgentScopeWorkflowPlan`。
4. `WorkflowPlanWriter` 写出审计产物到 `target/generated-workflows/{workflowId}`。
5. `WorkflowExecutor` 注册到 `WorkflowRegistry`。

调用：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/demo/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"ITEM-1001 status","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user"}'
```

### CLI 转换

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/default.dsl target/generated-workflows/default --source-type dify'
```

带本地执行：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/default.dsl target/generated-workflows/default --source-type dify --run 你好'
```

带真实集成：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/default.dsl target/generated-workflows/default --source-type dify --run 你好 --higress-mcp-endpoint https://gateway.example.com/mcp --knowledge-endpoint https://knowledge.example.com/retrieve'
```

## 手写 AgentScope YAML

适合不经过 Dify、直接维护 workflow。

最小 DAG：

```yaml
workflow:
  name: Manual AgentScope Demo
  mode: agentscope-workflow
  variables:
    - name: chat_round
      type: number
      default: 0

graph:
  nodes:
    - id: start
      type: start
      title: Start
    - id: assistant
      type: agent
      title: Assistant
      instruction: "Answer the user concisely."
      model: dashscope:qwen-plus
      tools: []
    - id: answer
      type: answer
      title: Answer
      answer: "{{#assistant.text#}}"
  edges:
    - from: start
      to: assistant
      handle: source
    - from: assistant
      to: answer
      handle: source
```

配置：

```yaml
dify:
  agentscope:
    workflows:
      manual-demo:
        source-type: agentscope
        location: classpath:workflows/manual-demo.workflow.yml
```

CLI 运行：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/manual-demo.workflow.yml target/generated-workflows/manual-demo --source-type agentscope --run 你好'
```

## 从零创建 workflow

### Java code-first 创建

如果不使用 Dify、不使用 YAML，也不是通过 HTTP 模板“一句话创建”，推荐使用 code-first API。这个模式最接近 AgentScope 官方示例：在 Java 里逐步声明 Agent、Tool、Memory 和 Knowledge，然后注册到本项目运行时。默认应使用 `official(...)`，它会走 `agentscope-react`，不生成 DAG 的 start/edge/answer。

Spring Boot 中可以这样写：

```java
import com.example.dify2agentscope.application.workflow.CodeFirstWorkflowBuilder;
import com.example.dify2agentscope.application.workflow.CodeFirstWorkflowRuntime;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupportWorkflowConfig {

    @Bean
    ApplicationRunner supportWorkflow(CodeFirstWorkflowRuntime runtime) {
        return args -> runtime.register(CodeFirstWorkflowBuilder.official("support-code", "Support Code Workflow")
            .variable("chat_round", "number", 0, "Conversation round")
            .agent("assistant", "SupportAssistant", "Answer support questions.", "dashscope:qwen-plus", agent -> agent
                .timeout(Duration.ofSeconds(30))
                .memos(5)
                .knowledge(Map.of("dataset_ids", java.util.List.of("support")))
                .tool("lookup_demo_item", "Lookup Demo Item", "Lookup demo item by id", "higress", Map.of()))
            .build());
    }
}
```

注册后这个 workflow 和 Dify/YAML 导入的 workflow 完全一样：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/support-code/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"ITEM-1001 status","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user"}'
```

`react(...)` 是 `official(...)` 的兼容别名：

```java
@Bean
ApplicationRunner reactSupportWorkflow(CodeFirstWorkflowRuntime runtime) {
    return args -> runtime.register(CodeFirstWorkflowBuilder.react("support-react", "Support React")
        .agent("assistant", "SupportAssistant", "Answer support questions.", "dashscope:qwen-plus", agent -> agent
            .timeout(Duration.ofSeconds(30))
            .tool("lookup_demo_item", "Lookup Demo Item", "Lookup demo item by id", "higress", Map.of()))
        .build());
}
```

code-first 复用的现有能力：

- `WorkflowRegistry`: 注册后自动进入 `/api/v1/workflows/{workflowId}/chat`、`/stream`、A2A。
- `WorkflowExecutor`: 官方入口走 ReAct 自循环；DAG 入口复用迁移兼容图执行、节点超时、异常记录、answer 模板。
- `ToolGateway`: 复用 Higress MCP 或后续官方 MCP 适配。
- `KnowledgeRetriever`: 复用知识库 HTTP 集成。
- `MemoStore`: 复用 Memos 长期记忆。
- `SessionMemoryStore`: 复用短期会话记忆。
- `ExecutionTracer`: 复用 traceId、节点开始/完成/失败日志。
- `PermissionPolicy` / `OutputSanitizer`: 复用安全边界和输出净化。
- `AgentStateStore` / `RuntimeContext`: 真实 Agent 模式下复用官方状态隔离。

选择建议：

- 要完整 Java 手写，优先用 `CodeFirstWorkflowBuilder` + `CodeFirstWorkflowRuntime`。
- 官方风格从零创建，优先用 `CodeFirstWorkflowBuilder.official(...)`。
- 只有迁移后需要立刻保留外部节点图时，才用 `CodeFirstWorkflowBuilder.dag(...)`。
- 只是临时从 UI/API 快速创建一个最小 Agent，才用 HTTP 模板。
- 已有 Dify 应用迁移，才走 Dify DSL。
- 想用文本配置交付 workflow，才走 AgentScope YAML。

### HTTP 模板创建

如果只是快速创建最小 workflow，可以调用模板接口：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/templates/agent \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowId": "support-demo",
    "name": "Support Demo",
    "executionMode": "react",
    "agentId": "assistant",
    "agentTitle": "SupportAssistant",
    "instruction": "Answer support questions concisely.",
    "model": "dashscope:qwen-plus",
    "memoRetrieval": true,
    "variables": {
      "chat_round": 0
    }
  }'
```

说明：

- `executionMode` 默认为 `dag`。
- `executionMode=react` 会创建 `agentscope-react` 自循环 workflow。
- `memoRetrieval=true` 会在 Agent 前插入 Memos 检索节点。
- 创建后立即注册到 `WorkflowRegistry`，并写出到 `target/generated-workflows/{workflowId}`。

### 底层 WorkflowBuilder

`WorkflowBuilder` 仍保留为底层兼容构建器，主要给测试、迁移适配和内部服务使用。它只产出 `WorkflowPlan`，
不包含 `workflowId` 和运行时注册动作；新业务代码不要把它作为主入口，优先使用 `CodeFirstWorkflowBuilder`
和 `CodeFirstWorkflowRuntime`。

## 中间件和外部能力接入

### Higress MCP / Tool

配置：

```yaml
dify:
  agentscope:
    higress-mcp-endpoint: https://your-higress.example.com/mcp
    higress-mcp-bearer-token: optional_token
```

对应配置属性绑定到 `DifyRuntimeProperties#higressMcpEndpoint` 和 `DifyRuntimeProperties#higressMcpBearerToken`。

运行链路：

1. Agent 工具声明来自 Dify DSL 或 YAML `tools`。
2. `AgentScopeAgentFactory` 把工具挂到官方 `Toolkit`。
3. `DifyMcpToolStub` 作为 AgentScope Tool 外壳。
4. 真实调用进入 `ToolGateway`。
5. 默认实现 `HigressMcpToolGateway` 发送 MCP JSON-RPC `tools/call`。

扩展方式：

- 如果要接官方 MCP Client，新增 `ToolGateway` 实现或在 `AgentScopeAgentFactory` 中注册官方 MCP client。
- 不要绕开 `PermissionPolicy`，工具调用前必须校验 Agent 声明的工具白名单。

### Knowledge HTTP

配置：

```yaml
dify:
  agentscope:
    knowledge-endpoint: https://your-knowledge.example.com/retrieve
    knowledge-bearer-token: optional_token
```

运行链路：

- `knowledge-retrieval` 节点调用 `KnowledgeRetriever`。
- 默认真实实现是 `HttpKnowledgeRetriever`。
- 未配置且允许 Stub 时使用 `StubKnowledgeRetriever`。

请求体大致包含：

```json
{
  "query": "user query",
  "dataset_ids": [],
  "retrieval_mode": "multiple",
  "multiple_retrieval_config": {}
}
```

### Memos 长期记忆

配置：

```yaml
dify:
  agentscope:
    memory:
      memos:
        enabled: true
        endpoint: http://127.0.0.1:5230
        access-token: your_token
        visibility: PRIVATE
        default-limit: 5
```

使用方式：

- YAML 中添加 `memo-retrieval` 节点。
- HTTP 模板创建时传 `"memoRetrieval": true`。
- 手动写入：`POST /api/v1/memory/memos`。
- 手动搜索：`GET /api/v1/memory/memos?userId=user&query=xxx&limit=5`。

未启用时：

- `memo-retrieval` 检索返回空结果。
- 手动写入会报 `Memos integration is not configured`。

### Session Memory

配置：

```yaml
dify:
  agentscope:
    memory:
      session:
        enabled: true
        max-turns: 20
```

隔离键：

```text
workflowId + userId + x_dify_chat_id
```

说明：

- 这是项目级短期 conversation 兼容层。
- AgentScope 官方状态使用共享 `AgentStateStore`，由 `RuntimeContext.userId/sessionId` 隔离。
- 当前默认内存实现不持久化，生产要替换成 Redis / DB 时实现 `SessionMemoryStore`。

### Nacos

配置：

```yaml
dify:
  agentscope:
    nacos:
      enabled: true
      server-addr: 127.0.0.1:8848
      namespace: public
      username: nacos
      password: nacos
      prompt:
        keys: ["demo_prompt"]
      skill:
        names: ["demo_skill"]
      mcp:
        servers:
          demo:
            data-id: demo-mcp.json
            group: DEFAULT_GROUP
```

查看同步结果：

```bash
curl --noproxy '*' http://127.0.0.1:8080/api/v1/nacos/resources
```

边界：

- Prompt / Skill / MCP 资源进入 `NacosResourceCatalog`。
- 当前不会假设 Nacos 能全量列出 skill，必须显式配置名称。
- 后续要把 skill 接到 AgentScope 官方 Skill，应从 `NacosResourceCatalog` 做映射，不要让业务代码直接依赖 Nacos SDK。

## 监控和观测

### traceId

请求可以传 `trace_id`：

```json
{
  "query": "你好",
  "x_dify_chat_id": "chat",
  "x_menu_id": "menu",
  "user_id": "user",
  "trace_id": "trace-demo"
}
```

不传时系统自动生成 UUID。

响应包含：

- `traceId`
- `executionPath`
- `nodeOutputs`
- `success`
- `error`

日志 MDC 包含：

```text
[traceId:...,workflowId:...]
```

核心实现：

- `WorkflowExecutionContext`
- `ExecutionTracer`
- `LoggingExecutionTracer`

### 节点观测

每个节点输出会追加：

```json
{
  "_status": "success",
  "_duration_ms": 12
}
```

失败时：

```json
{
  "_status": "failed",
  "_duration_ms": 1001,
  "_error": {
    "nodeId": "assistant",
    "nodeType": "agent",
    "nodeTitle": "Assistant",
    "durationMs": 1001,
    "timeoutMs": 1000,
    "errorType": "WorkflowNodeTimeoutException",
    "message": "Node assistant timed out after 1000ms"
  }
}
```

### Actuator

默认暴露：

```http
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

生产建议：

- 将 actuator 放在内网或网关鉴权后。
- 接 Prometheus 时增加 `micrometer-registry-prometheus` 并开放 `/actuator/prometheus`。
- 如果要把节点耗时打成指标，新增 `ExecutionTracer` 实现，在 `nodeFinished/nodeFailed` 中记录 timer/counter。

### A2A 观测

A2A task 结果 metadata 会包含：

- `executionPath`
- `traceId`

接口：

```http
GET /api/v1/a2a/tasks/{taskId}
```

## 常见问题排障

### 启动失败

优先看：

- `fail-on-missing-integrations`
- `boundary.allow-stub-integrations`
- Higress MCP endpoint 是否配置
- Knowledge endpoint 是否配置
- Memos enabled 时 endpoint/token 是否配置
- Nacos enabled 时 server-addr 是否配置

本地开发建议：

```yaml
dify:
  agentscope:
    build-agents: false
    fail-on-missing-integrations: false
    boundary:
      allow-stub-integrations: true
```

生产建议：

```yaml
dify:
  agentscope:
    build-agents: true
    fail-on-missing-integrations: true
    boundary:
      allow-stub-integrations: false
```

### 404 或 Unknown workflowId

检查：

- `dify.agentscope.workflows.<workflowId>` 是否配置。
- `source-type` 是否正确。
- `location` / `dsl-location` 是否存在。
- 动态创建 workflow 是否已经调用 `/api/v1/workflows/templates/agent`。

查看已注册 workflow：

```bash
curl --noproxy '*' http://127.0.0.1:8080/api/v1/workflows
```

### 400 Bad Request

通常是 HTTP 请求体缺少必填字段：

- `query`
- `x_dify_chat_id`
- `x_menu_id`
- `user_id`

如果不希望强制 userId，调整：

```yaml
dify:
  agentscope:
    boundary:
      require-user-id: false
```

### 422 success=false

说明 workflow 已执行但业务失败。优先看响应：

- `error`
- `executionPath`
- `nodeOutputs.<nodeId>._error`

再用 `traceId` 查日志。

### 节点超时

默认 30 秒。配置：

```yaml
dify:
  agentscope:
    execution:
      default-node-timeout: 30s
      node-timeouts:
        agent: 45s
        assistant: 20s
```

优先级：

```text
节点 timeout-ms / timeoutMs / timeout
> execution.node-timeouts.<nodeId>
> execution.node-timeouts.<nodeType>
> execution.default-node-timeout
```

### 没有真实 LLM 输出

检查 `build-agents`：

- `false`: 使用 `StubAgentInvoker`，输出包含 `[AgentName stub]`。
- `true`: 构建官方 AgentScope `ReActAgent`。

如果 `build-agents=true` 仍异常：

- 检查模型配置是否被 `AgentScopeAgentFactory#toAgentScopeModel` 支持。
- 检查工具权限和 MCP 连接。
- 检查 `RuntimeContext` 中 `userId/sessionId` 是否符合预期。

### MCP 工具调用失败

检查：

- Agent 的工具是否在 DSL/YAML 中声明。
- Higress 是否支持 `tools/call`。
- Bearer Token 是否正确。
- 响应是否为 JSON-RPC JSON 或 SSE `data:` JSON。
- `PermissionPolicy` 是否拒绝了未声明工具。

相关类：

- `DifyMcpToolStub`
- `ToolExecutionScope`
- `HigressMcpToolGateway`

### Knowledge 检索为空

检查：

- 是否配置 `knowledge-endpoint`。
- 本地是否仍在使用 `StubKnowledgeRetriever`。
- 知识库响应是否包含 `result` 字段。
- Dify DSL 中 `dataset_ids` 是否为空或不正确。

### Memos 检索为空或写入失败

检查：

- `memory.memos.enabled=true`
- `endpoint` 和 `access-token` 已配置。
- Memos API 版本是否兼容 `/api/v1/memos`。
- 检索 filter 是否被 Memos 支持。

### agentscope-harness 失败

这是当前预期行为。

`agentscope-harness` 只作为官方 Harness/Multi-Agent 的扩展入口，未接入官方能力前不会降级成单 Agent ReAct。要支持该模式，应新增官方 Harness 接入实现，而不是复用 `runReactSelfLoop`。

## 新增能力应该改哪里

| 目标 | 优先改动点 |
| --- | --- |
| 新增节点类型 | `WorkflowExecutor#executeNode`，再补 YAML/DSL 解析和测试 |
| 新增中间件 | 新增 domain interface 或实现现有 integration interface，避免控制器直接依赖 SDK |
| 替换 MCP 实现 | 实现 `ToolGateway` 或接入 AgentScope 官方 MCP client |
| 替换 session memory | 实现 `SessionMemoryStore` |
| 替换长期记忆 | 实现 `MemoStore` |
| 新增监控指标 | 新增 `ExecutionTracer` 实现或装饰 `LoggingExecutionTracer` |
| 新增 workflow 来源 | 在 `WorkflowRuntimeConfig#parseWorkflow` 和 CLI `parseWorkflow` 增加 source-type |
| 接官方 Harness | 新增 Harness invoker，替换 `WorkflowExecutor#isHarnessMode` 的显式失败 |

## 交付前检查清单

- `mvn test` 通过。
- 新 workflow 能通过 `/metadata` 查看节点数、边数、执行模式。
- 本地开发不误连生产中间件。
- 生产配置关闭 Stub。
- 每次请求有 `traceId`。
- 节点失败时响应中有 `_error`。
- 新增集成有超时配置和失败信息。
- 新增对外接口有参数校验。
