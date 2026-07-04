package com.example.dify2agentscope.domain.memory;

import java.util.List;

/**
 * 记忆笔记写入请求的记录类(Record)
 * 用于封装客户端提交的笔记写入请求的所有相关信息
 *
 * @param userId 用户ID，标识笔记的创建者
 * @param content 笔记的正文内容
 * @param tags 笔记相关的标签列表，用于分类和检索
 * @param visibility 笔记的可见性设置，控制谁能查看该笔记
 */
public record MemoWriteRequest(
        String userId,    // 创建笔记的用户标识
        String content,   // 笔记的文本内容
        List<String> tags, // 与笔记关联的标签列表
        String visibility) { // 笔记的可见性级别设置
}
