package com.example.dify2agentscope.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天请求 DTO —— 封装前端发起的聊天请求参数。
 * Chat request DTO — encapsulates parameters from the front-end chat request.
 */
public class ChatRequestDto {

    /** 用户查询内容 / user query text */
    @NotBlank
    @Size(max = 4000)
    private String query;

    /** Dify 聊天会话 ID / Dify chat session identifier */
    @NotBlank
    @JsonAlias("x_dify_chat_id")
    private String xDifyChatId;

    /** 菜单 ID / menu identifier */
    @NotBlank
    @JsonAlias("x_menu_id")
    private String xMenuId;

    /** 用户 ID / user identifier */
    @NotBlank
    @JsonAlias("user_id")
    private String userId;

    /** 追踪 ID / trace identifier */
    @JsonAlias("trace_id")
    private String traceId;

    /** 会话上下文信息 / conversation context */
    private Map<String, Object> conversation = new LinkedHashMap<>();

    /**
     * 获取用户查询内容。
     * Get the user query text.
     *
     * @return 查询内容 / query text
     */
    public String getQuery() {
        return query;
    }

    /**
     * 设置用户查询内容。
     * Set the user query text.
     *
     * @param query 查询内容 / query text
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * 获取 Dify 聊天会话 ID。
     * Get the Dify chat session identifier.
     *
     * @return 聊天会话 ID / chat session ID
     */
    public String getxDifyChatId() {
        return xDifyChatId;
    }

    /**
     * 设置 Dify 聊天会话 ID。
     * Set the Dify chat session identifier.
     *
     * @param xDifyChatId 聊天会话 ID / chat session ID
     */
    public void setxDifyChatId(String xDifyChatId) {
        this.xDifyChatId = xDifyChatId;
    }

    /**
     * 获取菜单 ID。
     * Get the menu identifier.
     *
     * @return 菜单 ID / menu ID
     */
    public String getxMenuId() {
        return xMenuId;
    }

    /**
     * 设置菜单 ID。
     * Set the menu identifier.
     *
     * @param xMenuId 菜单 ID / menu ID
     */
    public void setxMenuId(String xMenuId) {
        this.xMenuId = xMenuId;
    }

    /**
     * 获取用户 ID。
     * Get the user identifier.
     *
     * @return 用户 ID / user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     * Set the user identifier.
     *
     * @param userId 用户 ID / user ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取追踪 ID。
     * Get the trace identifier.
     *
     * @return 追踪 ID / trace ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 设置追踪 ID。
     * Set the trace identifier.
     *
     * @param traceId 追踪 ID / trace ID
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * 获取会话上下文信息。
     * Get the conversation context.
     *
     * @return 会话上下文 Map / conversation context map
     */
    public Map<String, Object> getConversation() {
        return conversation;
    }

    /**
     * 设置会话上下文信息。
     * Set the conversation context.
     *
     * @param conversation 会话上下文 Map，为 null 时初始化为空 Map /
     *                     conversation context map, initialized to empty map if null
     */
    public void setConversation(Map<String, Object> conversation) {
        this.conversation = conversation == null ? new LinkedHashMap<>() : new LinkedHashMap<>(conversation);
    }
}
