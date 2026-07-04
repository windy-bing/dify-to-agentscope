package com.example.dify2agentscope.application.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.dify.DifyDslParser;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link WorkflowExecutor} 的单元测试。
 * <p>覆盖三种 demo identifier 类型（item/request/task）的工作流执行，验证节点输出包含预期的 demo 信息。
 * 每次测试会创建全新的 executor 实例，避免 conversation state 跨测试污染。
 */
class WorkflowExecutorTest {

    private static final String ITEM_AGENT_ID = "demo-item-agent";
    private static final String REQUEST_AGENT_ID = "demo-request-agent";
    private static final String TASK_AGENT_ID = "demo-task-agent";
    private static final String FALLBACK_AGENT_ID = "demo-fallback-agent";

    private WorkflowPlan plan;

    /** 每次测试前解析 DSL 作为 plan */
    @BeforeEach
    void setUp() throws Exception {
        try (InputStream input = WorkflowExecutorTest.class.getResourceAsStream("/workflows/default.dsl")) {
            plan = new DifyDslParser().parse(input);
        }
    }

    /** 创建全新的 WorkflowExecutor 实例，配置 stub 实现和 demo identifier 正则 */
    private WorkflowExecutor newExecutor() {
        return new WorkflowExecutor(
                "test",
                plan,
                new StubAgentInvoker(),
                new StubKnowledgeRetriever(),
                new PermissionPolicy(),
                new OutputSanitizer(),
                new LoggingExecutionTracer(),
                100,
                "当前系统繁忙，无法联网工作，请稍后重试！",
                "ITEM-\\d{4}",
                "REQ-\\d{4}",
                "TASK-\\d{4}");
    }

    /** demo item 查询应路由到 item Agent，并提取 demo item id 至 conversation */
    @Test
    void routesDemoItemQueryToItemAgent() {
        WorkflowExecutor executor = newExecutor();
        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "ITEM-1001 status", "chat", "menu", "user", Map.of()));

        assertTrue(response.success(), "response should be successful");
        assertEquals("ITEM-1001", response.conversation().get("demo_item_id"));
        assertTrue(response.answer().contains("DemoItemAgent"));
        assertTrue(response.executionPath().contains(ITEM_AGENT_ID));
    }

    /** demo request 查询应路由到 request Agent，并提取 demo request id 至 conversation */
    @Test
    void routesDemoRequestQueryToRequestAgent() {
        WorkflowExecutor executor = newExecutor();
        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "REQ-2001 owner", "chat", "menu", "user", Map.of()));

        assertTrue(response.success(), "response should be successful");
        assertEquals("REQ-2001", response.conversation().get("demo_request_id"));
        assertTrue(response.answer().contains("DemoRequestAgent"));
        assertTrue(response.executionPath().contains(REQUEST_AGENT_ID));
    }

    /** demo task 查询应路由到 task Agent，并提取 demo task id 至 conversation */
    @Test
    void routesDemoTaskQueryToTaskAgent() {
        WorkflowExecutor executor = newExecutor();
        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "TASK-3001 progress", "chat", "menu", "user", Map.of()));

        assertTrue(response.success(), "response should be successful");
        assertEquals("TASK-3001", response.conversation().get("demo_task_id"));
        assertTrue(response.answer().contains("DemoTaskAgent"));
        assertTrue(response.executionPath().contains(TASK_AGENT_ID));
    }

    /** 无 demo identifier 的输入应路由到兜底 Agent */
    @Test
    void routesNoIdentifierQueryToFallbackAgent() {
        WorkflowExecutor executor = newExecutor();
        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "你好", "chat", "menu", "user", Map.of()));

        assertTrue(response.success(), "response should be successful");
        assertTrue(response.answer().contains("DemoFallbackAgent"));
        assertTrue(response.executionPath().contains(FALLBACK_AGENT_ID));
    }

    /** 缺少 x-menu-id 应返回失败和错误消息 */
    @Test
    void rejectsMissingMenuId() {
        WorkflowExecutor executor = newExecutor();
        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "你好",
                "chat",
                "",
                "user",
                Map.of()));

        assertFalse(response.success());
        assertEquals("x_menu_id is required", response.error());
    }

    /** 节点超过默认处理时长时应失败，并在失败节点输出中记录结构化异常 */
    @Test
    void failsNodeWhenDefaultTimeoutExceeded() {
        WorkflowPlan timeoutPlan = new WorkflowPlan(
                "timeout",
                "agentscope-workflow",
                "",
                List.of(),
                List.of(
                        new DifyNode("start", "start", "Start", Map.of()),
                        new DifyNode("slow", "knowledge-retrieval", "Slow", Map.of()),
                        new DifyNode("answer", "answer", "Answer", Map.of("answer", "ok"))),
                List.of(
                        new DifyEdge("start-source-slow", "start", "source", "slow"),
                        new DifyEdge("slow-source-answer", "slow", "source", "answer")),
                List.of());
        WorkflowExecutor executor = new WorkflowExecutor(
                "timeout-test",
                timeoutPlan,
                new StubAgentInvoker(),
                (node, context) -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Map.of("result", "late");
                },
                new PermissionPolicy(),
                new OutputSanitizer(),
                new LoggingExecutionTracer(),
                10,
                Duration.ofMillis(10),
                Map.of(),
                "fallback",
                "ITEM-\\d{4}",
                "REQ-\\d{4}",
                "TASK-\\d{4}");

        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "慢查询", "chat", "menu", "user", Map.of()));

        assertFalse(response.success());
        assertTrue(response.error().contains("timed out"));
        assertEquals("failed", response.nodeOutputs().get("slow").get("_status"));
        assertTrue(response.nodeOutputs().get("slow").containsKey("_error"));
    }

    /** AgentScope ReAct 自循环模式不依赖 DAG 边，应直接调用第一个 Agent */
    @Test
    void reactSelfLoopModeInvokesFirstAgentWithoutDagEdges() {
        WorkflowPlan selfLoopPlan = WorkflowBuilder.createReactSelfLoop("self-loop")
                .agent("assistant", "Assistant", "Answer directly.", "dashscope:qwen-plus")
                .build();
        assertTrue(selfLoopPlan.nodes().stream().noneMatch(node -> "answer".equals(node.type())));
        WorkflowExecutor executor = new WorkflowExecutor(
                "self-loop",
                selfLoopPlan,
                new StubAgentInvoker(),
                new StubKnowledgeRetriever(),
                new PermissionPolicy(),
                new OutputSanitizer(),
                new LoggingExecutionTracer(),
                10,
                "fallback",
                "ITEM-\\d{4}",
                "REQ-\\d{4}",
                "TASK-\\d{4}");

        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "你好", "chat", "menu", "user", Map.of()));

        assertTrue(response.success());
        assertTrue(response.answer().contains("Assistant"));
        assertEquals(List.of("assistant"), response.executionPath());
    }

    /** Harness 模式在官方 Multi-Agent 接入前必须显式失败，不能退化成 ReAct 单 Agent 执行 */
    @Test
    void harnessModeFailsUntilOfficialHarnessIsIntegrated() {
        WorkflowPlan harnessPlan = new WorkflowPlan(
                "harness",
                "agentscope-harness",
                "",
                List.of(),
                List.of(
                        new DifyNode("start", "start", "Start", Map.of()),
                        new DifyNode("assistant", "agent", "Assistant", Map.of("type", "agent"))),
                List.of(new DifyEdge("start-source-assistant", "start", "source", "assistant")),
                List.of(new AgentSpec(
                        "assistant",
                        "Assistant",
                        "Answer directly.",
                        "dashscope:qwen-plus",
                        Map.of(),
                        List.of())));
        WorkflowExecutor executor = new WorkflowExecutor(
                "harness",
                harnessPlan,
                new StubAgentInvoker(),
                new StubKnowledgeRetriever(),
                new PermissionPolicy(),
                new OutputSanitizer(),
                new LoggingExecutionTracer(),
                10,
                "fallback",
                "ITEM-\\d{4}",
                "REQ-\\d{4}",
                "TASK-\\d{4}");

        WorkflowResponse response = executor.execute(new WorkflowRequest(
                "你好", "chat", "menu", "user", Map.of()));

        assertFalse(response.success());
        assertTrue(response.error().contains("official Harness/Multi-Agent integration"));
    }
}
