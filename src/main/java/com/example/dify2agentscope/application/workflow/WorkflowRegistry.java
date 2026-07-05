package com.example.dify2agentscope.application.workflow;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 按 workflowId 管理已加载的工作流运行时。
 * workflowId 是多个 Dify DSL 对外隔离的边界。
 */
public class WorkflowRegistry {

    private final ConcurrentMap<String, WorkflowRuntimeBundle> workflows;

    /**
     * 构造工作流注册表。
     * Constructs the workflow registry.
     *
     * @param workflows 工作流运行时集合 / map of workflow runtime bundles
     */
    public WorkflowRegistry(Map<String, WorkflowRuntimeBundle> workflows) {
        this.workflows = new ConcurrentHashMap<>(workflows);
    }

    /**
     * 根据 workflowId 获取对应的运行时包，不存在时抛出异常。
     * Retrieves the runtime bundle by workflowId, throwing an exception if not found.
     *
     * @param workflowId 工作流标识 / workflow identifier
     * @return 工作流运行时包 / workflow runtime bundle
     * @throws IllegalArgumentException 未知的 workflowId / unknown workflowId
     */
    public WorkflowRuntimeBundle get(String workflowId) {
        WorkflowRuntimeBundle bundle = workflows.get(workflowId);
        if (bundle == null) {
            throw new IllegalArgumentException("Unknown workflowId: " + workflowId);
        }
        return bundle;
    }

    /**
     * 返回所有已注册的工作流标识集合。
     * Returns the set of all registered workflow identifiers.
     *
     * @return 工作流 ID 集合 / set of workflow IDs
     */
    public Set<String> workflowIds() {
        return Set.copyOf(workflows.keySet());
    }

    /**
     * 动态注册或覆盖一个 workflow 运行时包。
     *
     * @param workflowId workflow ID
     * @param bundle     workflow 运行时包
     */
    public void register(String workflowId, WorkflowRuntimeBundle bundle) {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId must not be blank");
        }
        workflows.put(workflowId, Objects.requireNonNull(bundle, "bundle must not be null"));
    }
}
