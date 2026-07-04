package com.example.dify2agentscope.domain.security;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运行时边界校验集中入口。
 * <p>
 * 请求校验和工具白名单校验都放在这里，保证 HTTP、A2A 和后续入口复用同一套权限行为。
 * Central runtime permission policy — validates workflow requests and tool whitelist access,
 * ensuring that HTTP, A2A, and future entry points share a consistent permission model.
 */
public class PermissionPolicy {

    /** 查询参数最大长度 / Maximum query length */
    private final int maxQueryLength;

    /** 是否强制要求用户 ID / Whether user ID is required */
    private final boolean requireUserId;

    /**
     * 使用默认配置构造权限策略（查询长度上限 4000，不强制用户 ID）。
     * Creates a policy with default settings (max query length 4000, user ID not required).
     */
    public PermissionPolicy() {
        this(4000, false);
    }

    /**
     * 使用指定配置构造权限策略。
     * Creates a policy with the specified settings.
     *
     * @param maxQueryLength 查询参数最大长度 / maximum query length
     * @param requireUserId  是否强制要求用户 ID / whether user ID is required
     */
    public PermissionPolicy(int maxQueryLength, boolean requireUserId) {
        this.maxQueryLength = maxQueryLength;
        this.requireUserId = requireUserId;
    }

    /**
     * 校验工作流请求的合法性。
     * <p>
     * 检查 query、x_dify_chat_id、x_menu_id 是否为空，query 是否超过长度上限，
     * 以及在 requireUserId 为 true 时检查 user_id 是否为空。
     * Validates the workflow request — checks that query, x_dify_chat_id, x_menu_id are present,
     * query does not exceed the maximum length, and user ID is present when required.
     *
     * @param request 工作流请求 / workflow request
     * @throws WorkflowSecurityException 校验失败时抛出 / thrown when validation fails
     */
    public void validateRequest(WorkflowRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new WorkflowSecurityException("sys.query is required");
        }
        if (request.query().length() > maxQueryLength) {
            throw new WorkflowSecurityException("sys.query exceeds " + maxQueryLength + " characters");
        }
        if (request.xDifyChatId() == null || request.xDifyChatId().isBlank()) {
            throw new WorkflowSecurityException("x_dify_chat_id is required");
        }
        if (request.xMenuId() == null || request.xMenuId().isBlank()) {
            throw new WorkflowSecurityException("x_menu_id is required");
        }
        if (requireUserId && (request.userId() == null || request.userId().isBlank())) {
            throw new WorkflowSecurityException("user_id is required");
        }
    }

    /**
     * 校验工具的调用权限。
     * <p>
     * 检查工具是否在 Agent 声明的白名单内，同时复用 #validateRequest 的请求级校验。
     * Validates that the tool is declared on the agent's tool whitelist, and also delegates
     * to #validateRequest for request-level checks.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @param agent   Agent 规格 / agent specification
     * @param tool    待调用的工具规格 / tool specification to validate
     * @throws WorkflowSecurityException 校验失败时抛出 / thrown when validation fails
     */
    public void validateToolCall(WorkflowExecutionContext context, AgentSpec agent, ToolSpec tool) {
        Set<String> allowed = agent.tools().stream().map(ToolSpec::name).collect(Collectors.toSet());
        if (!allowed.contains(tool.name())) {
            throw new WorkflowSecurityException("Tool is not declared on agent: " + tool.name());
        }
        validateRequest(context.request());
    }
}
