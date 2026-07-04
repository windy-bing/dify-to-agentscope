package com.example.dify2agentscope.infrastructure.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.dify2agentscope.application.workflow.WorkflowExecutor;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentScopeWorkflowParserTest {

    @Test
    void parsesAndExecutesNativeWorkflow() throws Exception {
        WorkflowPlan plan;
        try (InputStream input = getClass().getResourceAsStream("/workflows/manual-demo.workflow.yml")) {
            plan = new AgentScopeWorkflowParser().parse(input);
        }

        assertEquals("Manual AgentScope Demo", plan.appName());
        assertEquals("agentscope-workflow", plan.mode());
        assertEquals(1, plan.agents().size());

        WorkflowExecutor executor = new WorkflowExecutor(
                "manual-demo",
                plan,
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
                "hello",
                "chat",
                "menu",
                "user",
                Map.of()));

        assertTrue(response.success());
        assertTrue(response.answer().contains("ManualAssistant"));
        assertEquals("answer", response.executionPath().get(response.executionPath().size() - 1));
    }
}
