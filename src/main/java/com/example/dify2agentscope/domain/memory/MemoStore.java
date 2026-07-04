package com.example.dify2agentscope.domain.memory;

import java.util.List;

/**
 * MemoStore 接口定义了备忘录存储和搜索的基本操作
 * 该接口提供了保存备忘录和搜索备忘录两个核心功能
 */
public interface MemoStore {

    /**
     * 保存备忘录的方法
     * @param request 包含备忘录写入信息的请求对象
     * @return MemoRecord 保存后的备忘录记录，包含生成的ID等信息
     */
    MemoRecord save(MemoWriteRequest request);

    /**
     * 搜索备忘录的方法
     * @param request 包含搜索条件的请求对象
     * @return List<MemoRecord> 符合搜索条件的备忘录记录列表
     */
    List<MemoRecord> search(MemoSearchRequest request);
}
