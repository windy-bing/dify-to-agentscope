package com.example.dify2agentscope.domain.memory;

import java.time.Instant;
import java.util.List;

/**
 * 这是一个备忘录记录类，使用Java record类型定义
 * record类型是一种不可变的数据载体，自动生成equals、hashCode、toString等方法
 */
public record MemoRecord(
        // 备忘录名称，字符串类型
        String name,
        // 备忘录内容，字符串类型
        String content,
        // 备忘录标签列表，字符串列表类型
        List<String> tags,
        // 备忘录可见性，字符串类型
        String visibility,
        // 备忘录创建时间，Instant类型
        Instant createTime) {
}
