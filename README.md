# Dify to AgentScope

这个工程是 AgentScope Java / Spring Boot workflow 运行时。Dify `advanced-chat` DSL 只是导入格式之一；导入后会先归一到 AgentScope 规范模型，再按 DAG 兼容模式或 ReAct 自循环模式执行。

## 当前能力

- 从 `src/main/resources/workflows/default.dsl` 读取默认 Dify DSL。
- 支持 `source-type: dify` 自动转换和 `source-type: agentscope` 手动创建 workflow。
- 支持没有 YAML/DSL 时通过 Java code-first 方式从零创建 workflow，并复用现有中间件、执行器、观测和 HTTP/A2A 入口。
- 支持短期 session memory，并可选接入 Memos 作为长期 Markdown 记忆库。
- 按 `workflowId` 隔离多个 workflow；HTTP 地址复用同一组接口。
- 解析 Dify app、变量、graph 节点、边、agent 节点、MCP 工具与知识库节点，并转换为 AgentScope 规范模型。
- 支持 `agentscope-workflow` DAG 兼容执行和 `agentscope-react` ReAct 自循环执行；`agentscope-harness` 保留官方 Harness/Multi-Agent 扩展入口，未接入前会明确失败。
- 通过 Spring Boot 暴露普通响应、SSE 流式响应、A2A Agent/Task 接口。
- 通过 Higress MCP 网关调用 Dify 工具声明。
- 通过 HTTP 调用知识库检索。
- 支持 Nacos 获取 prompt、skill、MCP 服务配置；配置了才同步。
- 每个工作流和节点都有链路日志，使用 `traceId` 串联请求、节点执行和 A2A 任务。
- CLI 支持一键从 DSL 生成最小化 `GeneratedAgents` 骨架，生成物输出到 `target/generated-workflows/{workflowId}`。

依赖只使用 Maven Central，不依赖父工程。

## 运行服务

```bash
mvn spring-boot:run
```

普通响应：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/default/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"你好","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user","trace_id":"trace-demo"}'
```

SSE 流式响应：

```bash
curl --noproxy '*' -N -X POST http://127.0.0.1:8080/api/v1/workflows/default/stream \
  -H 'Content-Type: application/json' \
  -d '{"query":"你好","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user","trace_id":"trace-demo"}'
```

## 生成骨架

默认从 `src/main/resources/workflows/default.dsl` 生成到 `target/generated-workflows/default`：

```bash
mvn -q exec:java
```

指定 DSL 和输出目录：

```bash
mvn -q exec:java -Dexec.args='src/main/resources/workflows/default.dsl target/generated-workflows/default'
```

生成的 `GeneratedAgents.java` 是最小生产骨架，不嵌入 DSL 里的 prompt、节点内容或工具内容；运行时会先把入口兼容计划转换为 `AgentScopeWorkflowPlan`，再构建 AgentScope agent。

## 多 Workflow

多个 workflow 共用 HTTP 服务，通过 `workflowId` 选择运行图：

```yaml
dify:
  agentscope:
    workflows:
      demo-prod:
        source-type: dify
        dsl-location: classpath:workflows/default.dsl
      manual-demo:
        source-type: agentscope
        location: classpath:workflows/manual-demo.workflow.yml
```

调用时使用：

```http
POST /api/v1/workflows/{workflowId}/chat
POST /api/v1/workflows/{workflowId}/stream
```

## 生产集成

Higress MCP 和知识库 HTTP 通过配置注入：

```yaml
dify:
  agentscope:
    higress-mcp-endpoint: https://your-higress.example.com/mcp
    higress-mcp-bearer-token: optional_token
    knowledge-endpoint: https://your-knowledge.example.com/retrieve
    knowledge-bearer-token: optional_token
```

生产环境建议关闭 stub 并开启启动校验：

```yaml
dify:
  agentscope:
    fail-on-missing-integrations: true
    boundary:
      allow-stub-integrations: false
```

## Nacos

Nacos 默认关闭。开启后会同步已配置的 prompt、skill、MCP 服务配置：

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

查看当前同步结果：

```http
GET /api/v1/nacos/resources
```

说明：本地 AgentScope Java 的 Nacos skill 扩展需要配置已知 skill 名称，Nacos AI 当前没有可靠的 list-all-skill 接口；因此本工程采用“配置了就获取”的生产边界。

## A2A

A2A 对外暴露的是整个 workflow，不是每个内部 LLM 节点：

```http
GET /api/v1/a2a/agents
GET /api/v1/a2a/agents/{agentId}
POST /api/v1/a2a/tasks
GET /api/v1/a2a/tasks/{taskId}
```

内部 LLM 节点会体现在 AgentCard skills 和执行链路日志里。

## 从零创建

没有 Dify、没有 YAML/DSL，也不想通过模板“一句话创建”时，推荐在 Java 里按 AgentScope 官方风格逐步声明 workflow：

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

注册后复用同一套 HTTP、SSE、A2A、MCP、Knowledge、Memos、Trace、节点超时、异常记录和安全边界：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/support-code/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"你好","x_dify_chat_id":"chat","x_menu_id":"menu","user_id":"user","trace_id":"trace-demo"}'
```

如果只是 UI/API 快速创建一个最小 workflow，可以使用模板接口。`executionMode` 默认为 `dag`，传 `react` 会创建 AgentScope ReAct 自循环 workflow：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/v1/workflows/templates/agent \
  -H 'Content-Type: application/json' \
  -d '{"workflowId":"support-demo","name":"Support Demo","executionMode":"react","agentId":"assistant","agentTitle":"Assistant"}'
```

## 文档

- `docs/onboarding-runbook.md`
- `docs/architecture.md`
- `docs/workflow-authoring.md`
- `docs/mcp-compatibility.md`
- `docs/a2a.md`
