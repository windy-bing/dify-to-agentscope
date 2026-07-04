package com.example.dify2agentscope.adapter.in.a2a;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.application.workflow.WorkflowExecutor;
import com.example.dify2agentscope.application.workflow.WorkflowRegistry;
import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.application.a2a.A2aService;
import com.example.dify2agentscope.domain.a2a.A2aTaskStore;
import com.example.dify2agentscope.infrastructure.a2a.InMemoryA2aTaskStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(A2aController.class)
@Import(A2aControllerTest.TestBeans.class)
class A2aControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesWorkflowAsAgentCard() throws Exception {
        mockMvc.perform(get("/api/v1/a2a/agents/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("default"))
                .andExpect(jsonPath("$.name").value("app"))
                .andExpect(jsonPath("$.skills[0].name").value("DemoAgent"));
    }

    @Test
    void createsAndReadsTask() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/a2a/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "default",
                                  "input": "你好",
                                  "metadata": {
                                    "x_dify_chat_id": "chat",
                                    "x_menu_id": "menu",
                                    "user_id": "user"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.answer").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String taskId = body.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/v1/a2a/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        A2aTaskStore a2aTaskStore() {
            return new InMemoryA2aTaskStore();
        }

        @Bean
        WorkflowRegistry workflowRegistry() {
            WorkflowPlan plan = new WorkflowPlan(
                    "app",
                    "advanced-chat",
                    "opening",
                    List.of(),
                    List.of(
                            new DifyNode("start", "start", "start", Map.of()),
                            new DifyNode("answer", "answer", "answer", Map.of("answer", "ok"))),
                    List.of(new DifyEdge("edge", "start", "source", "answer")),
                    List.of(new AgentSpec("agent-node", "DemoAgent", "instruction", "qwen", null, List.of())));
            WorkflowExecutor executor = new WorkflowExecutor(
                    plan,
                    new StubAgentInvoker(),
                    new StubKnowledgeRetriever(),
                    new PermissionPolicy(),
                    new OutputSanitizer());
            return new WorkflowRegistry(Map.of("default", new WorkflowRuntimeBundle("default", plan, executor)));
        }

        @Bean
        A2aService a2aService(WorkflowRegistry workflowRegistry, A2aTaskStore taskStore) {
            return new A2aService(workflowRegistry, taskStore);
        }
    }
}
