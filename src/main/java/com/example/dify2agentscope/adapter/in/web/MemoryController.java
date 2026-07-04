package com.example.dify2agentscope.adapter.in.web;

import com.example.dify2agentscope.domain.memory.MemoRecord;
import com.example.dify2agentscope.domain.memory.MemoSearchRequest;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.memory.MemoWriteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 长期记忆 HTTP 控制器。
 * <p>对外提供 Memos 写入和检索能力；当运行时未启用 Memos 时，底层 {@link MemoStore} 会按 NoOp 策略处理。</p>
 */
@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {

    private final MemoStore memoStore;

    /**
     * 创建长期记忆控制器。
     *
     * @param memoStore 长期记忆存储适配器
     */
    public MemoryController(MemoStore memoStore) {
        this.memoStore = memoStore;
    }

    /**
     * 写入一条长期记忆。
     *
     * @param request 写入请求
     * @return 写入后的记忆记录
     */
    @PostMapping("/memos")
    public MemoRecord saveMemo(@Valid @RequestBody SaveMemoRequest request) {
        return memoStore.save(new MemoWriteRequest(
                request.userId(),
                request.content(),
                request.tags() == null ? List.of() : request.tags(),
                request.visibility()));
    }

    /**
     * 按用户和查询词检索长期记忆。
     *
     * @param userId 用户 ID
     * @param query  查询词
     * @param limit  最大返回条数
     * @return 包含 memos 数组的响应 Map
     */
    @GetMapping("/memos")
    public Map<String, Object> searchMemos(
            @RequestParam("userId") String userId,
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return Map.of("memos", memoStore.search(new MemoSearchRequest(userId, query, limit)));
    }

    /**
     * 写入 Memos 的请求体。
     *
     * @param userId     用户 ID
     * @param content    记忆 Markdown 内容
     * @param tags       标签列表
     * @param visibility Memos 可见性
     */
    public record SaveMemoRequest(
            @NotBlank String userId,
            @NotBlank String content,
            List<String> tags,
            String visibility) {
    }
}
