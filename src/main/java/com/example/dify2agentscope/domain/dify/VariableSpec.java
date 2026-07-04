package com.example.dify2agentscope.domain.dify;

/**
 * Dify 工作流对话变量的定义。
 * <p>每个变量包含名称、类型、描述和默认值，在执行上下文初始化时注入 conversation。
 */
public record VariableSpec(
        String name,
        String valueType,
        String description,
        Object defaultValue) {
}
