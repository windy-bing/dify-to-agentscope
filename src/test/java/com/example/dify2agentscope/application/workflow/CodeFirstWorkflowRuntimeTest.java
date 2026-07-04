package com.example.dify2agentscope.application.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.mcp.StubToolGateway;
import com.example.dify2agentscope.infrastructure.memory.NoOpMemoStore;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * code-first workflow 运行时测试。
 * <p>验证不依赖 Dify DSL/YAML 的 Java 手写 workflow 也能注册进现有 runtime，并复用执行器能力。</p>
 */
class CodeFirstWorkflowRuntimeTest {

    @TempDir
    private Path tempDir;

    /** code-first DAG workflow 应注册到 registry，并能通过现有 executor 执行 */
    @Test
    void registersCodeFirstDagWorkflowIntoRuntime() {
        WorkflowRegistry registry = new WorkflowRegistry(Map.of());
        CodeFirstWorkflowRuntime runtime = newRuntime(registry);
        CodeFirstWorkflowDefinition definition = CodeFirstWorkflowBuilder.dag("code-support", "Code Support")
                .variable("chat_round", "number", 0, "Conversation round")
                .assignConstant("set-scope", "Set Scope", "scope", "support")
                .agent("assistant", "Assistant", "Answer support questions.", "dashscope:qwen-plus", agent -> agent
                        .timeout(Duration.ofSeconds(5))
                        .tool("lookup_demo_item", "Lookup Demo Item", "Lookup demo item information", "higress", Map.of()))
                .answer("{{#assistant.text#}}")
                .build();

        WorkflowRuntimeBundle bundle = runtime.register(definition);
        WorkflowResponse response = bundle.executor().execute(new WorkflowRequest(
                "你好", "chat", "menu", "user", Map.of()));

        assertEquals("code-support", bundle.workflowId());
        assertEquals("Code Support", bundle.plan().appName());
        assertTrue(response.success());
        assertTrue(response.answer().contains("Assistant"));
        assertEquals("support", response.conversation().get("scope"));
        assertTrue(registry.workflowIds().contains("code-support"));
    }

    /** code-first 官方风格 workflow 不应生成 DAG 节点，应直接调用第一个 Agent */
    @Test
    void registersOfficialCodeFirstWorkflowWithoutDagNodes() {
        WorkflowRegistry registry = new WorkflowRegistry(Map.of());
        CodeFirstWorkflowRuntime runtime = newRuntime(registry);
        WorkflowRuntimeBundle bundle = runtime.register(CodeFirstWorkflowBuilder.official("code-react", "Code React")
                .agent("assistant", "Assistant", "Answer directly.", "dashscope:qwen-plus", agent -> agent
                        .memos(3)
                        .knowledge(Map.of("dataset_ids", java.util.List.of("support"))))
                .build());

        WorkflowResponse response = registry.get("code-react").executor().execute(new WorkflowRequest(
                "你好", "chat", "menu", "user", Map.of()));

        assertEquals(1, bundle.plan().nodes().size());
        assertTrue(bundle.plan().edges().isEmpty());
        assertTrue(bundle.plan().nodes().stream().noneMatch(node -> "answer".equals(node.type())));
        assertTrue(bundle.plan().nodes().stream().noneMatch(node -> "start".equals(node.type())));
        assertTrue(response.success());
        assertEquals(java.util.List.of("assistant"), response.executionPath());
        assertEquals("", response.conversation().get("_agent_memos"));
        assertTrue(response.conversation().containsKey("_agent_knowledge"));
    }

    /**
     * 创建测试用 code-first runtime。
     *
     * @param registry workflow 注册表
     * @return code-first runtime
     */
    private CodeFirstWorkflowRuntime newRuntime(WorkflowRegistry registry) {
        DifyRuntimeProperties properties = new DifyRuntimeProperties();
        properties.setGeneratedOutputDir(tempDir.toString());
        properties.setBuildAgents(false);
        properties.getExecution().setDefaultNodeTimeout(Duration.ofSeconds(30));
        return new CodeFirstWorkflowRuntime(
                registry,
                properties,
                new StubToolGateway(),
                new StubKnowledgeRetriever(),
                new PermissionPolicy(),
                new OutputSanitizer(),
                new LoggingExecutionTracer(),
                new NoOpMemoStore(),
                new InMemoryAgentStateStore());
    }
}
