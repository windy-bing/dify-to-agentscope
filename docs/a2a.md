# A2A Mapping

外部 A2A 边界是整个 workflow。

## Mapping

- A2A Agent `id` = `workflowId`
- A2A Agent `name` = AgentScope workflow app name
- A2A Agent `description` = opening statement summary
- A2A skills = workflow 内部 agent 节点
- A2A task input = user query
- A2A task metadata = `x_dify_chat_id`, `x_menu_id`, `user_id`, `trace_id`
- A2A task result = `WorkflowResponse.answer`
- A2A task metadata includes execution path and `traceId` for trace/debug.

## API

```http
GET /api/v1/a2a/agents
GET /api/v1/a2a/agents/{agentId}
POST /api/v1/a2a/tasks
GET /api/v1/a2a/tasks/{taskId}
```

Create a task:

```json
{
  "agentId": "default",
  "input": "你好",
  "metadata": {
    "x_dify_chat_id": "chat",
    "x_menu_id": "menu",
    "user_id": "user",
    "trace_id": "trace-demo"
  },
  "conversation": {}
}
```

内部 agent 节点不会作为公开 A2A Agent 暴露。它们只作为 AgentCard skills 和执行链路元数据暴露。
