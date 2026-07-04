package com.example.dify2agentscope.domain.memory;

/**
 * 这是一个用于搜索备忘录的请求记录类（Java Record）
 * Record是Java 14中引入的新特性，用于创建不可变的数据载体类
 */
public record MemoSearchRequest(
    // 用户ID，用于标识发起搜索请求的用户
        String userId,
    // 搜索关键词，用于在备忘录中匹配相关内容
        String query,
    // 搜索结果的数量限制，控制返回的备忘录数量
        int limit) {
}
