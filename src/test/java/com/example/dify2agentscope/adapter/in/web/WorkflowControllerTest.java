package com.example.dify2agentscope.adapter.in.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dify2agentscope.application.workflow.WorkflowExecutor;
import com.example.dify2agentscope.application.workflow.WorkflowCreationService;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.application.workflow.WorkflowRegistry;
import com.example.dify2agentscope.application.workflow.WorkflowRuntimeBundle;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.application.workflow.WorkflowService;
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

/**
 * {@link WorkflowController} 的 Web MVC 切片测试。
 * <p>注入 MockMvc 并通过 TestBeans 提供 WorkflowService/WorkflowRegistry 桩，
 * 验证 Boundary 字段（snake_case）的反序列化、query 校验和 metadata 端点。
 */
@WebMvcTest(WorkflowController.class)
@Import(WorkflowControllerTest.TestBeans.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** snake_case Boundary 字段应正确反序列化为 ChatRequestDto */
    @Test
    void chatAcceptsSnakeCaseBoundaryFields() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/default/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "你好",
                                  "x_dify_chat_id": "chat",
                                  "x_menu_id": "menu",
                                  "user_id": "user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.answer").value("ok"));
    }

    /** 缺少 query 字段应返回 400 Bad Request */
    @Test
    void chatRejectsMissingQuery() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/default/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "x_dify_chat_id": "chat",
                                  "x_menu_id": "menu",
                                  "user_id": "user"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /** GET /metadata 应返回 workflows 的 appName/mode 元信息 */
    @Test
    void metadataExposesWorkflowShape() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/default/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value("default"))
                .andExpect(jsonPath("$.appName").value("app"))
                .andExpect(jsonPath("$.mode").value("advanced-chat"));
    }

    @Test
    void createsAgentWorkflowFromTemplate() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/templates/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowId": "new-flow",
                                  "name": "New Flow",
                                  "agentId": "assistant",
                                  "agentTitle": "Assistant"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value("new-flow"))
                .andExpect(jsonPath("$.appName").value("New Flow"));
    }

    /** AgentScope 能力清单端点应暴露官方能力和当前落地状态 */
    @Test
    void agentScopeCapabilitiesExposeOfficialAnchors() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/agentscope/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities[*].name", hasItem("CODE_FIRST_AUTHORING")))
                .andExpect(jsonPath("$.capabilities[*].name", hasItem("AGENT")))
                .andExpect(jsonPath("$.capabilities[?(@.name == 'AGENT')].officialConcept", hasItem("Agent / ReActAgent")))
                .andExpect(jsonPath("$.capabilities[?(@.name == 'AGENT')].status", hasItem("OFFICIAL_API_USED")));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        WorkflowService workflowService(WorkflowRegistry workflowRegistry) {
            return new WorkflowService(workflowRegistry) {
                @Override
                public WorkflowResponse execute(String workflowId, ChatRequestDto request) {
                    return new WorkflowResponse("ok", Map.of(), Map.of(), List.of(), "test-trace", true, "");
                }
            };
        }

        @Bean
        WorkflowRegistry workflowRegistry() {
            WorkflowPlan plan = new WorkflowPlan("app", "advanced-chat", "", List.of(), List.of(), List.of(), List.of());
            return new WorkflowRegistry(Map.of("default", new WorkflowRuntimeBundle("default", plan, null)));
        }

        @Bean
        WorkflowCreationService workflowCreationService(WorkflowRegistry workflowRegistry) {
            return new WorkflowCreationService() {
                @Override
                public WorkflowRuntimeBundle createAgentWorkflow(CreateAgentWorkflowRequest request) {
                    WorkflowPlan plan = new WorkflowPlan(
                            request.getName(),
                            "agentscope-workflow",
                            "",
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of());
                    return new WorkflowRuntimeBundle(request.getWorkflowId(), plan, null);
                }
            };
        }
    }
}
