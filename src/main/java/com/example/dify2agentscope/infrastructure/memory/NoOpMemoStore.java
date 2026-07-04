package com.example.dify2agentscope.infrastructure.memory;

import com.example.dify2agentscope.domain.memory.MemoRecord;
import com.example.dify2agentscope.domain.memory.MemoSearchRequest;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.memory.MemoWriteRequest;
import java.util.List;

/**
 * 未启用 Memos 时使用的空长期记忆存储。
 * <p>检索返回空列表，写入直接失败，避免调用方误以为记忆已经持久化。</p>
 */
public class NoOpMemoStore implements MemoStore {

    /**
     * 拒绝写入长期记忆。
     *
     * @param request 记忆写入请求
     * @return 不会返回
     */
    @Override
    public MemoRecord save(MemoWriteRequest request) {
        throw new IllegalStateException("Memos integration is not configured");
    }

    /**
     * 返回空的长期记忆检索结果。
     *
     * @param request 记忆检索请求
     * @return 空列表
     */
    @Override
    public List<MemoRecord> search(MemoSearchRequest request) {
        return List.of();
    }
}
