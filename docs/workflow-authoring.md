# Workflow Authoring

本项目支持多条创建 workflow 的路径，入口会先进入兼容 `WorkflowPlan`，随后归一成 `AgentScopeWorkflowPlan`，再由 `WorkflowExecutor` 按执行模式运行。

当前规范：Dify 只作为输入格式存在。无论来源是 Java code-first、Dify DSL、手写 YAML 还是 HTTP 模板，进入运行时后都会转换为
`AgentScopeWorkflowPlan`。DAG 只是迁移兼容模式，目标形态应逐步改造成 AgentScope ReAct 自循环或 Harness 多 Agent。

## 路径

1. Java code-first：适合没有 Dify、没有 YAML/DSL，并希望按 AgentScope 官方风格逐步写代码完成 workflow。
2. 自动转换 Dify DSL：适合已有 Dify 应用迁移。
3. 手动创建 AgentScope workflow YAML：适合需要文本化交付和审计 workflow。
4. HTTP 模板：适合 UI/API 快速创建最小 Agent，不作为复杂业务 workflow 的主路径。

## Spring 配置

```yaml
dify:
  agentscope:
    workflows:
      from-dify:
        source-type: dify
        dsl-location: classpath:workflows/default.dsl
      manual:
        source-type: agentscope
        location: classpath:workflows/manual-demo.workflow.yml
```

`dsl-location` 是兼容旧配置的字段。新 workflow 建议统一使用 `source-type` + `location`。

## 手写 Workflow 格式

最小示例：

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
      title: ManualAssistant
      timeout-ms: 30000
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

## 支持的节点

- `start`: 注入请求边界字段。
- `code`: 当前支持内置 identifier 提取和当前时间输出。
- `if-else`: 按条件返回分支 handle。
- `assigner`: 写入 conversation 变量。
- `knowledge-retrieval`: 调用 `KnowledgeRetriever`。
- `memo-retrieval`: 调用 Memos 长期记忆检索。
- `agent`: 对齐 AgentScope Java 的 `ReActAgent` 概念，配置 `title`、`instruction`、`model`、`tools`。
- `answer`: 用 `{{#node.field#}}` 模板渲染最终输出。

## 节点超时和异常记录

默认每个节点最多执行 30 秒。超过配置时长后，当前节点默认失败，workflow 返回 `success=false` 和兜底答案。

全局配置：

```yaml
dify:
  agentscope:
    execution:
      default-node-timeout: 30s
      node-timeouts:
        agent: 45s
        assistant: 20s
```

优先级：节点自身 `timeout-ms` / `timeoutMs` / `timeout` > `execution.node-timeouts.<nodeId>` > `execution.node-timeouts.<nodeType>` > `execution.default-node-timeout`。

节点输出会追加运行元信息，成功节点包含：

```json
{
  "_status": "success",
  "_duration_ms": 12
}
```

失败节点包含：

```json
{
  "_status": "failed",
  "_duration_ms": 1001,
  "_error": {
    "nodeId": "assistant",
    "nodeType": "agent",
    "nodeTitle": "ManualAssistant",
    "durationMs": 1001,
    "timeoutMs": 1000,
    "errorType": "WorkflowNodeTimeoutException",
    "message": "Node assistant timed out after 1000ms"
  }
}
```

## CLI

自动转换 Dify：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/default.dsl target/generated-workflows/default --source-type dify'
```

手写 workflow：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/manual-demo.workflow.yml target/generated-workflows/manual-demo --source-type agentscope --run 你好'
```

CLI 可用 `--default-node-timeout-ms 30000` 覆盖本地执行的默认节点超时。

## 和 AgentScope Java 的对齐点

- workflow 入口会先转换为 `AgentScopeWorkflowPlan`；DAG 是迁移兼容执行模式，不伪造 AgentScope 官方不存在的 workflow 核心类型。
- agent 节点映射到 AgentScope Java `ReActAgent.builder()`。
- tool 绑定映射到 `Toolkit`，运行时通过现有 `ToolGateway` 转发到 MCP/Higress 或 stub。
- Web/SSE/A2A 暴露的是整个 workflow，内部 agent 节点作为 workflow skills 和执行链路的一部分呈现。

## DAG 兼容和 ReAct 自循环

迁移 Dify 时优先保留 DAG，保证转换后能立即运行：

```yaml
workflow:
  mode: agentscope-workflow
```

改造成更符合 AgentScope 的自循环模式时，使用：

```yaml
workflow:
  mode: agentscope-react
```

`agentscope-react` 最小语义：不再走外部 DAG 边，运行时直接调用第一个 Agent，由官方 `ReActAgent` 自行完成
reasoning/acting/tool calling。复杂多 Agent 场景后续应走 `agentscope-harness`，对齐官方 Harness/Multi-Agent 能力；当前版本会对该模式明确失败，避免误当作已实现的官方多 Agent 编排。

## 从零创建

如果没有 YAML/DSL，推荐优先使用 Java code-first API：业务代码一步步声明官方 ReAct Agent，并通过
`CodeFirstWorkflowRuntime` 注册到现有运行时。注册后仍然复用 HTTP/A2A、MCP、Knowledge、Memos、Trace、
Timeout、Permission 等能力。主入口是 `official(...)`，它不会生成 DAG 的 start/edge/answer。

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

`react(...)` 是 `official(...)` 的兼容别名：

```java
@Bean
ApplicationRunner reactWorkflow(CodeFirstWorkflowRuntime runtime) {
    return args -> runtime.register(CodeFirstWorkflowBuilder.react("support-react", "Support React")
        .agent("assistant", "SupportAssistant", "Answer support questions.", "dashscope:qwen-plus", agent -> agent
            .timeout(Duration.ofSeconds(30))
            .tool("lookup_demo_item", "Lookup Demo Item", "Lookup demo item by id", "higress", Map.of()))
        .build());
}
```

如果只是快速创建最小 workflow，可以用模板接口：

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

生成的 workflow 会立即注册到运行时，并写到 `target/generated-workflows/{workflowId}`。

`WorkflowBuilder` 仍保留为底层兼容构建器，主要给测试、迁移适配和内部服务使用；新业务代码应优先使用带
`workflowId`、工具/知识/记忆声明和 runtime 注册能力的 `CodeFirstWorkflowBuilder`。

## Session Memory

服务端默认启用短期 session memory，key 为：

```text
workflowId + userId + x_dify_chat_id
```

执行前会自动加载历史 conversation，执行成功后保存本轮 `query/answer/traceId` 到 `_session_turns`，并维护 `history_user_query`。

配置：

```yaml
dify:
  agentscope:
    memory:
      session:
        enabled: true
        max-turns: 20
```

## Memos 长期记忆

Memos 作为长期 Markdown 记忆库接入。未配置时 `memo-retrieval` 返回空结果，手动写入接口会报错。

配置：

```yaml
dify:
  agentscope:
    memory:
      memos:
        enabled: true
        endpoint: http://127.0.0.1:5230
        access-token: ${MEMOS_ACCESS_TOKEN}
        visibility: PRIVATE
        default-limit: 5
```

手动写入 memo：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/memory/memos \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user",
    "content": "#agent-memory\\n用户偏好：回答要直接、工程化。",
    "tags": ["agent-memory", "preference"],
    "visibility": "PRIVATE"
  }'
```

搜索 memo：

```bash
curl --noproxy '*' 'http://127.0.0.1:8080/api/v1/memory/memos?userId=user&query=回答&limit=5'
```

workflow 中加入 `memo-retrieval` 节点后，执行器会在节点输出中写入：

```json
{
  "result": "- memo content...",
  "memos": []
}
```
