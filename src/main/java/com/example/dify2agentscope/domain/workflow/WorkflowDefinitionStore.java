package com.example.dify2agentscope.domain.workflow;

import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import java.util.Map;

/**
 * Store for dynamically created workflow definitions.
 */
public interface WorkflowDefinitionStore {

    /**
     * Load all persisted dynamic workflow definitions.
     *
     * @return workflowId to workflow plan map
     */
    Map<String, WorkflowPlan> loadAll();

    /**
     * Save or replace a workflow definition.
     *
     * @param workflowId workflow identifier
     * @param plan workflow plan
     */
    void save(String workflowId, WorkflowPlan plan);

    /**
     * Whether this store is backed by process-local memory only.
     *
     * @return true for memory-only stores
     */
    default boolean localOnly() {
        return false;
    }
}
