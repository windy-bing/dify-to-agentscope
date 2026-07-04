package com.example.dify2agentscope.infrastructure.dify;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.ToolSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将 WorkflowPlan 写入为 JSON、Markdown 摘要以及生成的 Java 源代码。
 * <p>
 * Write WorkflowPlan to JSON, Markdown summary, and generated Java source code.
 */
public class WorkflowPlanWriter {

    private final ObjectMapper jsonMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 将工作流计划写入指定输出目录。
     * <p>
     * Write the workflow plan to the specified output directory.
     *
     * @param plan      工作流计划 / workflow plan
     * @param outputDir 输出目录 / output directory
     * @throws IOException 文件写入失败 / if file writing fails
     */
    public void write(WorkflowPlan plan, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        jsonMapper.writeValue(outputDir.resolve("workflow-plan.json").toFile(), plan);
        Files.writeString(outputDir.resolve("workflow-summary.md"), markdown(plan));
        Files.writeString(outputDir.resolve("GeneratedAgents.java"), generatedAgentsJava(plan));
    }

    /**
     * 生成工作流计划的 Markdown 摘要。
     * <p>
     * Generate a Markdown summary of the workflow plan.
     *
     * @param plan 工作流计划 / workflow plan
     * @return Markdown 格式的摘要 / summary in Markdown format
     */
    private String markdown(WorkflowPlan plan) {
        Map<String, Long> counts = plan.nodes().stream()
                .collect(Collectors.groupingBy(DifyNode::type, Collectors.counting()));

        StringBuilder md = new StringBuilder();
        md.append("# ").append(plan.appName()).append("\n\n");
        md.append("- Mode: `").append(plan.mode()).append("`\n");
        md.append("- Nodes: `").append(plan.nodes().size()).append("`\n");
        md.append("- Edges: `").append(plan.edges().size()).append("`\n");
        md.append("- Conversation variables: `").append(plan.variables().size()).append("`\n\n");

        md.append("## Node Types\n\n");
        counts.forEach((type, count) -> md.append("- `").append(type).append("`: `")
                .append(count).append("`\n"));

        md.append("\n## Agents\n\n");
        for (AgentSpec agent : plan.agents()) {
            md.append("### ").append(agent.title()).append("\n\n");
            md.append("- Node id: `").append(agent.nodeId()).append("`\n");
            md.append("- Model: `").append(agent.model()).append("`\n");
            md.append("- Tools: `").append(agent.tools().size()).append("`\n\n");
            for (ToolSpec tool : agent.tools()) {
                md.append("- `").append(tool.name()).append("`: ")
                        .append(tool.description()).append("\n");
            }
            md.append("\n");
        }
        return md.toString();
    }

    /**
     * 生成 GeneratedAgents.java 源代码，提供从 WorkflowPlan 构建 Agent 的静态方法。
     * <p>
     * Generate GeneratedAgents.java source code with static methods to build agents from WorkflowPlan.
     *
     * @param plan 工作流计划 / workflow plan
     * @return Java 源代码 / Java source code
     */
    private String generatedAgentsJava(WorkflowPlan plan) {
        StringBuilder java = new StringBuilder();
        java.append("package generated;\n\n");
        java.append("import com.example.dify2agentscope.domain.dify.WorkflowPlan;\n");
        java.append("import com.example.dify2agentscope.domain.security.PermissionPolicy;\n");
        java.append("import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeAgentFactory;\n");
        java.append("import io.agentscope.core.ReActAgent;\n");
        java.append("import java.util.Map;\n\n");
        java.append("public final class GeneratedAgents {\n");
        java.append("    private GeneratedAgents() {}\n\n");
        java.append("    public static Map<String, ReActAgent> build(WorkflowPlan plan) {\n");
        java.append("        return build(plan, new PermissionPolicy());\n");
        java.append("    }\n\n");
        java.append("    public static Map<String, ReActAgent> build(WorkflowPlan plan, PermissionPolicy permissionPolicy) {\n");
        java.append("        return new AgentScopeAgentFactory(permissionPolicy).buildAgents(plan);\n");
        java.append("    }\n");
        java.append("}\n");
        return java.toString();
    }
}
