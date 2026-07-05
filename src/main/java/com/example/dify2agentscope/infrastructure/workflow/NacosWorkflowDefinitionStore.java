package com.example.dify2agentscope.infrastructure.workflow;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.workflow.WorkflowDefinitionStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Nacos-backed store for dynamically created workflow definitions.
 */
public class NacosWorkflowDefinitionStore implements WorkflowDefinitionStore {

    private static final long CONFIG_TIMEOUT_MS = 5_000L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Set<String>> ID_SET = new TypeReference<>() {};

    private final ConfigService configService;
    private final String dataIdPrefix;
    private final String indexDataId;
    private final String group;

    public NacosWorkflowDefinitionStore(DifyRuntimeProperties properties) {
        try {
            this.configService = NacosFactory.createConfigService(nacosProperties(properties.getNacos()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Nacos workflow definition store", e);
        }
        DifyRuntimeProperties.WorkflowStore workflowStore = properties.getWorkflowStore();
        this.dataIdPrefix = blankToDefault(workflowStore.getNacosDataIdPrefix(), "dify-agentscope-workflow-");
        this.indexDataId = blankToDefault(workflowStore.getNacosIndexDataId(), "dify-agentscope-workflows.json");
        this.group = blankToDefault(workflowStore.getNacosGroup(), "DEFAULT_GROUP");
    }

    @Override
    public Map<String, WorkflowPlan> loadAll() {
        try {
            Map<String, WorkflowPlan> result = new LinkedHashMap<>();
            for (String workflowId : loadIndex()) {
                String content = configService.getConfig(dataId(workflowId), group, CONFIG_TIMEOUT_MS);
                if (content == null || content.isBlank()) {
                    continue;
                }
                result.put(workflowId, JSON.readValue(content, WorkflowPlan.class));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load workflow definitions from Nacos", e);
        }
    }

    @Override
    public void save(String workflowId, WorkflowPlan plan) {
        try {
            configService.publishConfig(dataId(workflowId), group, JSON.writeValueAsString(plan));
            Set<String> ids = new LinkedHashSet<>(loadIndex());
            ids.add(workflowId);
            configService.publishConfig(indexDataId, group, JSON.writeValueAsString(ids));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save workflow definition to Nacos", e);
        }
    }

    private Set<String> loadIndex() throws Exception {
        String content = configService.getConfig(indexDataId, group, CONFIG_TIMEOUT_MS);
        if (content == null || content.isBlank()) {
            return Set.of();
        }
        return JSON.readValue(content, ID_SET);
    }

    private String dataId(String workflowId) {
        return dataIdPrefix + workflowId + ".json";
    }

    private Properties nacosProperties(DifyRuntimeProperties.Nacos nacos) {
        if (nacos == null || nacos.getServerAddr() == null || nacos.getServerAddr().isBlank()) {
            throw new IllegalArgumentException("Nacos server-addr is required for workflow-store.type=nacos");
        }
        Properties result = new Properties();
        result.put("serverAddr", nacos.getServerAddr());
        result.put("namespace", blankToDefault(nacos.getNamespace(), "public"));
        putIfNotBlank(result, "username", nacos.getUsername());
        putIfNotBlank(result, "password", nacos.getPassword());
        putIfNotBlank(result, "accessKey", nacos.getAccessKey());
        putIfNotBlank(result, "secretKey", nacos.getSecretKey());
        return result;
    }

    private void putIfNotBlank(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
