package com.example.dify2agentscope.infrastructure.dify;

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
 * 解析 Dify DSL（YAML）工作流定义文件，将其转换为领域模型 WorkflowPlan。
 * <p>
 * Parse Dify DSL (YAML) workflow definition files into domain model WorkflowPlan.
 */
public class DifyDslParser {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 从文件路径解析 Dify DSL 文件。
     * <p>
     * Parse Dify DSL from a file path.
     *
     * @param dslPath DSL 文件的路径 / path to the DSL file
     * @return 解析得到的工作流计划 / parsed workflow plan
     * @throws IOException 文件读取或 YAML 解析失败 / if file reading or YAML parsing fails
     */
    public WorkflowPlan parse(Path dslPath) throws IOException {
        JsonNode root = yamlMapper.readTree(dslPath.toFile());
        return parse(root);
    }

    /**
     * 从输入流解析 Dify DSL。
     * <p>
     * Parse Dify DSL from an input stream.
     *
     * @param inputStream 输入流 / input stream of DSL content
     * @return 解析得到的工作流计划 / parsed workflow plan
     * @throws IOException 流读取或 YAML 解析失败 / if stream reading or YAML parsing fails
     */
    public WorkflowPlan parse(InputStream inputStream) throws IOException {
        JsonNode root = yamlMapper.readTree(inputStream);
        return parse(root);
    }

    /**
     * 内部解析核心：将 JsonNode 根节点转换为 WorkflowPlan。
     * <p>
     * Internal parse core: convert JsonNode root into WorkflowPlan.
     *
     * @param root YAML 解析后的根节点 / root node after YAML parsing
     * @return 工作流计划 / workflow plan
     */
    private WorkflowPlan parse(JsonNode root) {
        JsonNode app = root.path("app");
        JsonNode workflow = root.path("workflow");
        JsonNode graph = workflow.path("graph");

        List<VariableSpec> variables = parseVariables(workflow.path("conversation_variables"));
        List<DifyNode> nodes = parseNodes(graph.path("nodes"));
        List<DifyEdge> edges = parseEdges(graph.path("edges"));
        List<AgentSpec> agents = parseAgents(graph.path("nodes"));

        return new WorkflowPlan(
                app.path("name").asText("Dify App"),
                app.path("mode").asText("advanced-chat"),
                workflow.path("features").path("opening_statement").asText(""),
                variables,
                nodes,
                edges,
                agents);
    }

    /**
     * 解析对话变量列表。
     * <p>
     * Parse conversation variables.
     *
     * @param variablesNode 变量节点 / variables JSON node
     * @return 变量定义列表 / list of variable definitions
     */
    private List<VariableSpec> parseVariables(JsonNode variablesNode) {
        List<VariableSpec> variables = new ArrayList<>();
        if (!variablesNode.isArray()) {
            return variables;
        }
        for (JsonNode item : variablesNode) {
            variables.add(new VariableSpec(
                    item.path("name").asText(),
                    item.path("value_type").asText(),
                    item.path("description").asText(""),
                    jsonCompatible(item.path("value"))));
        }
        return variables;
    }

    /**
     * 解析节点列表。
     * <p>
     * Parse node list.
     *
     * @param nodesNode 节点数组 / nodes JSON array
     * @return 节点定义列表 / list of node definitions
     */
    private List<DifyNode> parseNodes(JsonNode nodesNode) {
        List<DifyNode> nodes = new ArrayList<>();
        if (!nodesNode.isArray()) {
            return nodes;
        }
        for (JsonNode item : nodesNode) {
            JsonNode data = item.path("data");
            nodes.add(new DifyNode(
                    item.path("id").asText(),
                    data.path("type").asText(),
                    data.path("title").asText(),
                    jsonCompatible(data)));
        }
        return nodes;
    }

    /**
     * 解析边列表。
     * <p>
     * Parse edge list.
     *
     * @param edgesNode 边数组 / edges JSON array
     * @return 边定义列表 / list of edge definitions
     */
    private List<DifyEdge> parseEdges(JsonNode edgesNode) {
        List<DifyEdge> edges = new ArrayList<>();
        if (!edgesNode.isArray()) {
            return edges;
        }
        for (JsonNode item : edgesNode) {
            edges.add(new DifyEdge(
                    item.path("id").asText(),
                    item.path("source").asText(),
                    item.path("sourceHandle").asText(""),
                    item.path("target").asText()));
        }
        return edges;
    }

    /**
     * 从节点列表中提取类型为 "agent" 的 Agent 定义。
     * <p>
     * Extract Agent definitions with type "agent" from node list.
     *
     * @param nodesNode 节点数组 / nodes JSON array
     * @return Agent 定义列表 / list of agent specifications
     */
    private List<AgentSpec> parseAgents(JsonNode nodesNode) {
        List<AgentSpec> agents = new ArrayList<>();
        if (!nodesNode.isArray()) {
            return agents;
        }
        for (JsonNode item : nodesNode) {
            JsonNode data = item.path("data");
            if (!"agent".equals(data.path("type").asText())) {
                continue;
            }
            JsonNode params = data.path("agent_parameters");
            agents.add(new AgentSpec(
                    item.path("id").asText(),
                    data.path("title").asText(),
                    params.path("instruction").path("value").asText(""),
                    params.path("model").path("value").path("model").asText("dashscope:qwen-plus"),
                    jsonCompatible(params.path("model").path("value").path("completion_params")),
                    parseTools(params.path("tools").path("value"))));
        }
        return agents;
    }

    /**
     * 解析 Agent 绑定的工具列表。
     * <p>
     * Parse tool list bound to an agent.
     *
     * @param toolsNode 工具数组 / tools JSON array
     * @return 工具定义列表 / list of tool specifications
     */
    private List<ToolSpec> parseTools(JsonNode toolsNode) {
        List<ToolSpec> tools = new ArrayList<>();
        if (!toolsNode.isArray()) {
            return tools;
        }
        for (JsonNode item : toolsNode) {
            tools.add(new ToolSpec(
                    item.path("tool_name").asText(),
                    item.path("tool_label").asText(),
                    item.path("tool_description").asText(),
                    item.path("provider_name").asText(),
                    jsonCompatible(item.path("parameters"))));
        }
        return tools;
    }

    /**
     * 将 Jackson JsonNode 递归转换为 Java 原生类型（Map / List / String / Number / Boolean）。
     * <p>
     * Recursively convert Jackson JsonNode to native Java types (Map / List / String / Number / Boolean).
     *
     * @param node JSON 节点 / JSON node
     * @return 兼容的 Java 对象 / JSON-compatible Java object
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
