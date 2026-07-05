package com.example.dify2agentscope.application.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link WorkflowRegistry} 并发安全行为测试。
 * <p>动态创建 workflow 会在运行期写入注册表，因此注册表必须返回快照并拒绝非法注册。</p>
 */
class WorkflowRegistryTest {

    /** workflowIds 应返回快照，调用方不能通过集合视图反向修改注册表 */
    @Test
    void workflowIdsReturnsSnapshot() {
        WorkflowRegistry registry = new WorkflowRegistry(Map.of("default", bundle("default")));

        Set<String> ids = registry.workflowIds();
        registry.register("created", bundle("created"));

        assertTrue(ids.contains("default"));
        assertTrue(registry.workflowIds().contains("created"));
        assertThrows(UnsupportedOperationException.class, () -> ids.add("bad"));
    }

    /** 注册空 ID 或空 bundle 时应快速失败 */
    @Test
    void registerRejectsInvalidArguments() {
        WorkflowRegistry registry = new WorkflowRegistry(Map.of());

        assertThrows(IllegalArgumentException.class, () -> registry.register("", bundle("bad")));
        assertThrows(NullPointerException.class, () -> registry.register("bad", null));
    }

    /**
     * 创建最小 workflow runtime bundle。
     *
     * @param workflowId workflow ID
     * @return runtime bundle
     */
    private WorkflowRuntimeBundle bundle(String workflowId) {
        WorkflowPlan plan = new WorkflowPlan(workflowId, "advanced-chat", "", List.of(), List.of(), List.of(), List.of());
        return new WorkflowRuntimeBundle(workflowId, plan, null);
    }
}
