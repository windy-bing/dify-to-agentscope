package com.example.dify2agentscope.infrastructure.workflow;

import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.workflow.WorkflowDefinitionStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process-local dynamic workflow definition store.
 */
public class InMemoryWorkflowDefinitionStore implements WorkflowDefinitionStore {

    private final ConcurrentMap<String, WorkflowPlan> workflows = new ConcurrentHashMap<>();

    @Override
    public Map<String, WorkflowPlan> loadAll() {
        return Map.copyOf(workflows);
    }

    @Override
    public void save(String workflowId, WorkflowPlan plan) {
        workflows.put(workflowId, plan);
    }

    @Override
    public boolean localOnly() {
        return true;
    }
}
