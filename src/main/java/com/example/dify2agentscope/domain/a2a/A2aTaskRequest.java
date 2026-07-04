package com.example.dify2agentscope.domain.a2a;

import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2A 任务请求。
 * <p>
 * 客户端提交任务时传入的参数，包含目标 Agent ID、输入文本、元数据和会话信息。
 * A2A task request — encapsulates the parameters passed by a client when submitting a task,
 * including the target agent ID, input text, metadata, and conversation context.
 */
public class A2aTaskRequest {

    /** 目标 Agent ID / Target agent identifier */
    @NotBlank
    private String agentId;

    /** 用户输入文本 / User input text */
    @NotBlank
    private String input;

    /** 附加元数据 / Additional metadata */
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /** 会话上下文 / Conversation context */
    private Map<String, Object> conversation = new LinkedHashMap<>();

    /**
     * 获取目标 Agent ID。
     * Returns the target agent ID.
     *
     * @return Agent ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 设置目标 Agent ID。
     * Sets the target agent ID.
     *
     * @param agentId Agent ID
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * 获取用户输入文本。
     * Returns the user input text.
     *
     * @return 用户输入 / user input
     */
    public String getInput() {
        return input;
    }

    /**
     * 设置用户输入文本。
     * Sets the user input text.
     *
     * @param input 用户输入 / user input
     */
    public void setInput(String input) {
        this.input = input;
    }

    /**
     * 获取附加元数据。
     * Returns the additional metadata.
     *
     * @return 元数据映射 / metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 设置附加元数据，传入 null 时重置为空映射。
     * Sets the additional metadata; resets to an empty map when null is passed.
     *
     * @param metadata 元数据映射 / metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    /**
     * 获取会话上下文。
     * Returns the conversation context.
     *
     * @return 会话映射 / conversation map
     */
    public Map<String, Object> getConversation() {
        return conversation;
    }

    /**
     * 设置会话上下文，传入 null 时重置为空映射。
     * Sets the conversation context; resets to an empty map when null is passed.
     *
     * @param conversation 会话映射 / conversation map
     */
    public void setConversation(Map<String, Object> conversation) {
        this.conversation = conversation == null ? new LinkedHashMap<>() : new LinkedHashMap<>(conversation);
    }
}
