# MCP / Higress / Nacos Compatibility

## Higress MCP

运行时通过 HTTP MCP 网关调用 Higress，发送 JSON-RPC `tools/call`：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "tool.name",
    "arguments": {}
  }
}
```

网关需要满足：

- 接受 `POST` 和 `Content-Type: application/json`。
- 支持标准 MCP `tools/call`。
- 返回 JSON-RPC JSON，或 SSE 且至少一个 `data:` 行包含 JSON 对象。
- 鉴权可以使用 `Authorization: Bearer ...` 和静态扩展 header。

如果 Higress 部署为 Streamable HTTP 并需要 session negotiation，应保留 `ToolGateway` 接口，替换 `HigressMcpToolGateway` 为 session-aware 实现。

## Nacos MCP 配置

Nacos 侧 MCP 当前按配置同步：

```yaml
dify:
  agentscope:
    nacos:
      enabled: true
      mcp:
        servers:
          demo:
            endpoint: https://gateway.example.com/mcp
          sample:
            data-id: sample-mcp.json
            group: DEFAULT_GROUP
```

- `endpoint` 直接作为 MCP 服务地址进入资源目录。
- `data-id` 会通过 Nacos ConfigService 拉取配置内容。
- 同步结果可通过 `GET /api/v1/nacos/resources` 查看。

## Prompt / Skill

Prompt 通过 Nacos AI `subscribePrompt` 获取：

```yaml
dify:
  agentscope:
    nacos:
      prompt:
        keys: ["demo_prompt"]
```

Skill 通过 Nacos AI skill 下载接口获取：

```yaml
dify:
  agentscope:
    nacos:
      skill:
        names: ["demo_skill"]
```

本地 AgentScope Java Nacos skill 扩展同样要求配置已知 skill 名称；没有依赖“全量列出 skill”的假设。

## Knowledge HTTP

知识库检索是普通 HTTP POST：

```json
{
  "query": "user query",
  "dataset_ids": [],
  "retrieval_mode": "multiple",
  "multiple_retrieval_config": {}
}
```

响应可以包含 `result` 字段；如果没有 `result`，会把有效 JSON 作为上下文转交给 agent。
