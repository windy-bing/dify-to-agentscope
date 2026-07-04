package com.example.dify2agentscope.infrastructure.workflow;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.dify.VariableSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手写 AgentScope workflow YAML 解析器。
 * <p>该解析器读取项目定义的最小 YAML 格式，并转换为入口兼容 {@link WorkflowPlan}。</p>
 */
public class AgentScopeWorkflowParser {

    private static final String DEFAULT_MODEL = "dashscope:qwen-plus";

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 从本地路径解析 workflow YAML。
     *
     * @param path YAML 文件路径
     * @return 兼容工作流计划
     * @throws IOException 文件读取或 YAML 解析失败时抛出
     */
    public WorkflowPlan parse(Path path) throws IOException {
        return parse(yamlMapper.readTree(path.toFile()));
    }

    /**
     * 从输入流解析 workflow YAML。
     *
     * @param inputStream YAML 输入流
     * @return 兼容工作流计划
     * @throws IOException 输入流读取或 YAML 解析失败时抛出
     */
    public WorkflowPlan parse(InputStream inputStream) throws IOException {
        return parse(yamlMapper.readTree(inputStream));
    }

    /**
     * 将 YAML 根节点转换为工作流计划。
     *
     * @param root YAML 根节点
     * @return 兼容工作流计划
     */
    private WorkflowPlan parse(JsonNode root) {
        JsonNode workflow = root.path("workflow");
        JsonNode graph = root.path("graph");
        JsonNode nodesNode = graph.path("nodes");

        List<DifyNode> nodes = parseNodes(nodesNode);
        return new WorkflowPlan(
                workflow.path("name").asText("AgentScope Workflow"),
                workflow.path("mode").asText("agentscope-workflow"),
                workflow.path("opening_statement").asText(""),
                parseVariables(workflow.path("variables")),
                nodes,
                parseEdges(graph.path("edges")),
                parseAgents(nodesNode));
    }

    /**
     * 解析 workflow.variables 配置。
     *
     * @param variablesNode variables 节点
     * @return 变量定义列表
     */
    private List<VariableSpec> parseVariables(JsonNode variablesNode) {
        List<VariableSpec> variables = new ArrayList<>();
        if (!variablesNode.isArray()) {
            return variables;
        }
        for (JsonNode item : variablesNode) {
            variables.add(new VariableSpec(
                    item.path("name").asText(),
                    item.path("type").asText("string"),
                    item.path("description").asText(""),
                    jsonCompatible(item.path("default"))));
        }
        return variables;
    }

    /**
     * 解析 graph.nodes 配置。
     *
     * @param nodesNode nodes 节点
     * @return 节点定义列表
     */
    private List<DifyNode> parseNodes(JsonNode nodesNode) {
        List<DifyNode> nodes = new ArrayList<>();
        if (!nodesNode.isArray()) {
            return nodes;
        }
        for (JsonNode item : nodesNode) {
            String id = item.path("id").asText();
            String type = item.path("type").asText();
            String title = item.path("title").asText(id);
            Map<String, Object> data = objectMap(item);
            data.put("type", type);
            data.put("title", title);
            nodes.add(new DifyNode(id, type, title, data));
        }
        return nodes;
    }

    /**
     * 解析 graph.edges 配置。
     *
     * @param edgesNode edges 节点
     * @return 边定义列表
     */
    private List<DifyEdge> parseEdges(JsonNode edgesNode) {
        List<DifyEdge> edges = new ArrayList<>();
        if (!edgesNode.isArray()) {
            return edges;
        }
        for (JsonNode item : edgesNode) {
            String source = item.path("from").asText(item.path("source").asText());
            String target = item.path("to").asText(item.path("target").asText());
            String handle = item.path("handle").asText(item.path("sourceHandle").asText("source"));
            String id = item.path("id").asText(source + "-" + handle + "-" + target);
            edges.add(new DifyEdge(id, source, handle, target));
        }
        return edges;
    }

    /**
     * 从 agent 节点中解析 Agent 定义。
     *
     * @param nodesNode nodes 节点
     * @return Agent 定义列表
     */
    private List<AgentSpec> parseAgents(JsonNode nodesNode) {
        List<AgentSpec> agents = new ArrayList<>();
        if (!nodesNode.isArray()) {
            return agents;
        }
        for (JsonNode item : nodesNode) {
            if (!"agent".equals(item.path("type").asText())) {
                continue;
            }
            agents.add(new AgentSpec(
                    item.path("id").asText(),
                    item.path("title").asText(item.path("id").asText()),
                    item.path("instruction").asText(""),
                    item.path("model").asText(DEFAULT_MODEL),
                    jsonCompatible(item.path("completion_params")),
                    parseTools(item.path("tools"))));
        }
        return agents;
    }

    /**
     * 解析 Agent 工具定义。
     *
     * @param toolsNode tools 节点
     * @return 工具定义列表
     */
    private List<ToolSpec> parseTools(JsonNode toolsNode) {
        List<ToolSpec> tools = new ArrayList<>();
        if (!toolsNode.isArray()) {
            return tools;
        }
        for (JsonNode item : toolsNode) {
            String name = item.path("name").asText();
            tools.add(new ToolSpec(
                    name,
                    item.path("label").asText(name),
                    item.path("description").asText(""),
                    item.path("provider").asText("agentscope"),
                    jsonCompatible(item.path("parameters"))));
        }
        return tools;
    }

    /**
     * 将 JSON 对象节点转换为可变 Map。
     *
     * @param node JSON 节点
     * @return 字符串键 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(JsonNode node) {
        Object value = jsonCompatible(node);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 将 Jackson 节点转换为普通 Java 值。
     *
     * @param node JSON/YAML 节点
     * @return Map/List/String/Number/Boolean/null 等 Java 值
     */
    private Object jsonCompatible(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                values.put(field.getKey(), jsonCompatible(field.getValue()));
            }
            return values;
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode item : node) {
                values.add(jsonCompatible(item));
            }
            return values;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt() || node.isLong()) {
            return node.asLong();
        }
        if (node.isFloat() || node.isDouble() || node.isBigDecimal()) {
            return node.asDouble();
        }
        return node.asText();
    }
}
