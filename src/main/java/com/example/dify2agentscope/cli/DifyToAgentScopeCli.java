package com.example.dify2agentscope.cli;

import com.example.dify2agentscope.infrastructure.dify.DifyDslParser;
import com.example.dify2agentscope.infrastructure.dify.WorkflowPlanWriter;
import com.example.dify2agentscope.infrastructure.workflow.AgentScopeWorkflowParser;
import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeAgentFactory;
import com.example.dify2agentscope.infrastructure.agentscope.AgentScopeStreamingAgentInvoker;
import com.example.dify2agentscope.infrastructure.mcp.HigressMcpToolGateway;
import com.example.dify2agentscope.infrastructure.knowledge.HttpKnowledgeRetriever;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.workflow.RuntimeConfig;
import com.example.dify2agentscope.infrastructure.agentscope.StubAgentInvoker;
import com.example.dify2agentscope.infrastructure.knowledge.StubKnowledgeRetriever;
import com.example.dify2agentscope.infrastructure.mcp.StubToolGateway;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import com.example.dify2agentscope.domain.integration.ToolGateway;
import com.example.dify2agentscope.application.workflow.WorkflowExecutor;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.InMemoryAgentStateStore;
import java.net.URI;
import java.time.Duration;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dify → AgentScope 命令行转换与执行工具。
 * <p>
 * 无需 Spring 容器即可独立运行的 CLI 入口，支持：
 * <ul>
 *   <li>解析 Dify DSL 文件并生成 AgentScope 工作流代码</li>
 *   <li>指定参数构建真实 AgentScope ReActAgent</li>
 *   <li>转换后立即通过 {@code --run} 执行一次用户请求</li>
 * </ul>
 * <p>
 * 参数格式：位置参数（DSL 路径、输出目录） + 长选项（{@code --build-agents}、
 * {@code --run <query>}、{@code --higress-mcp-endpoint <url>} 等）。
 */
public class DifyToAgentScopeCli {

    private static final Path DEFAULT_DSL = Path.of("src/main/resources/workflows/default.dsl");
    private static final Path DEFAULT_OUTPUT = Path.of("target/generated-workflows/default");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 主入口方法。
     * <p>
     * 解析命令行参数 → 读取并转换 Dify DSL → 写入生成目录 →
     * 可选构建 AgentScope Agent → 打印摘要信息 → 可选执行一次查询并输出 JSON。
     *
     * @param args 命令行参数 / command line arguments
     * @throws Exception 任何解析 / IO / 执行异常
     */
    public static void main(String[] args) throws Exception {
        CliArgs cli = CliArgs.parse(args);
        if (cli.hasFlag("help")) {
            printUsage();
            return;
        }
        Path workflowPath = cli.positionals().size() > 0 ? Path.of(cli.positionals().get(0)) : DEFAULT_DSL;
        Path outputDir = cli.positionals().size() > 1 ? Path.of(cli.positionals().get(1)) : DEFAULT_OUTPUT;
        boolean buildAgents = cli.hasFlag("build-agents");
        String sourceType = cli.optionOrDefault("source-type", "dify");
        String workflowId = cli.optionOrDefault("workflow-id", outputDir.getFileName().toString());

        WorkflowPlan plan = parseWorkflow(sourceType, workflowPath);
        new WorkflowPlanWriter().write(plan, outputDir);

        RuntimeConfig runtimeConfig = runtimeConfig(cli);
        ToolGateway toolGateway = runtimeConfig.higressMcpEndpoint().isPresent()
                ? new HigressMcpToolGateway(runtimeConfig)
                : new StubToolGateway();
        KnowledgeRetriever knowledgeRetriever = runtimeConfig.knowledgeEndpoint().isPresent()
                ? new HttpKnowledgeRetriever(runtimeConfig)
                : new StubKnowledgeRetriever();

        Map<String, ReActAgent> agents = Map.of();
        int agentCount = plan.agents().size();
        if (buildAgents) {
            agents = new AgentScopeAgentFactory(
                    new PermissionPolicy(),
                    new InMemoryAgentStateStore()).buildAgents(plan);
            agentCount = agents.size();
        }

        System.out.printf("已加载工作流: %s%n", plan.appName());
        System.out.printf("Workflow ID: %s%n", workflowId);
        System.out.printf("来源类型: %s%n", sourceType);
        System.out.printf("节点数: %d, 边数: %d, AgentScope Agent 数: %d%n",
                plan.nodes().size(), plan.edges().size(), agentCount);
        System.out.printf("生成目录: %s%n", outputDir.toAbsolutePath().normalize());
        if (!buildAgents) {
            System.out.println("未构建真实 ReActAgent；如需实例化 Agent，请配置模型密钥后增加 --build-agents。");
        }
        for (AgentSpec agent : plan.agents()) {
            System.out.printf("- %s: model=%s, tools=%d%n",
                    agent.title(), agent.model(), agent.tools().size());
        }

        String runQuery = cli.option("run");
        if (runQuery != null) {
            AgentInvoker invoker = buildAgents
                    ? new AgentScopeStreamingAgentInvoker(agents, toolGateway)
                    : new StubAgentInvoker();
            WorkflowExecutor executor = new WorkflowExecutor(
                    workflowId,
                    plan,
                    invoker,
                    knowledgeRetriever,
                    new PermissionPolicy(),
                    new OutputSanitizer(),
                    new LoggingExecutionTracer(),
                    100,
                    Duration.ofMillis(Long.parseLong(cli.optionOrDefault("default-node-timeout-ms", "30000"))),
                    Map.of(),
                    "当前系统繁忙，无法联网工作，请稍后重试！",
                    "ITEM-\\d{4}",
                    "REQ-\\d{4}",
                    "TASK-\\d{4}");
            WorkflowResponse response = executor.execute(new WorkflowRequest(
                    runQuery,
                    cli.optionOrDefault("x-dify-chat-id", "local-chat"),
                    cli.optionOrDefault("x-menu-id", "local-menu"),
                    cli.optionOrDefault("user-id", "local-user"),
                    Map.of()));
            System.out.println(JSON.writeValueAsString(response));
        }
    }

    /**
     * 打印 CLI 用法帮助信息到标准输出。
     * <p>
     * 包含所有位置参数和可选长选项的说明。
     */
    private static void printUsage() {
        System.out.println("""
                用法:
                   mvn exec:java
                   mvn exec:java -Dexec.args="src/main/resources/workflows/default.dsl target/generated-workflows/default"

                参数:
                   第 1 个位置参数: workflow 文件路径，默认 src/main/resources/workflows/default.dsl
                   第 2 个位置参数: 生成目录，默认 target/generated-workflows/default
                   --source-type <dify|agentscope>  来源类型，默认 dify
                   --workflow-id <id>  执行和日志使用的 workflowId，默认取生成目录名
                   --build-agents   同时构建 AgentScope ReActAgent
                   --run <query>    转换后执行一次本地请求
                   --higress-mcp-endpoint <url>    Higress MCP 地址
                   --knowledge-endpoint <url>      知识库 HTTP 地址
                   --request-timeout-ms <ms>       外部请求超时时间，默认 10000
                   --default-node-timeout-ms <ms>  单个节点默认超时时间，默认 30000
                   --help           打印帮助
                 """);
    }

    /**
     * 按来源类型解析 workflow 文件。
     *
     * @param sourceType   来源类型，支持 dify / agentscope 等别名
     * @param workflowPath workflow 文件路径
     * @return 兼容工作流计划
     * @throws Exception 文件读取或解析失败时抛出
     */
    private static WorkflowPlan parseWorkflow(String sourceType, Path workflowPath) throws Exception {
        return switch (sourceType.toLowerCase()) {
            case "dify", "dify-dsl" -> new DifyDslParser().parse(workflowPath);
            case "agentscope", "native", "workflow" -> new AgentScopeWorkflowParser().parse(workflowPath);
            default -> throw new IllegalArgumentException("Unsupported workflow source type: " + sourceType);
        };
    }

    /**
     * 从 CLI 参数构造 {@link RuntimeConfig}。
     * <p>
     * 读取 {@code --higress-mcp-endpoint}、{@code --higress-mcp-token}、
     * {@code --knowledge-endpoint}、{@code --knowledge-token} 以及
     * {@code --request-timeout-ms} 选项，封装为运行时配置。
     *
     * @param cli 解析后的 CLI 参数 / parsed CLI arguments
     * @return 运行时配置 / runtime configuration
     */
    private static RuntimeConfig runtimeConfig(CliArgs cli) {
        return new RuntimeConfig(
                optionalUri(cli.option("higress-mcp-endpoint")),
                optionalString(cli.option("higress-mcp-token")),
                optionalUri(cli.option("knowledge-endpoint")),
                optionalString(cli.option("knowledge-token")),
                Duration.ofMillis(Long.parseLong(cli.optionOrDefault("request-timeout-ms", "10000"))),
                Duration.ofMillis(Long.parseLong(cli.optionOrDefault("default-node-timeout-ms", "30000"))),
                Map.of(),
                Map.of());
    }

    /**
     * 将可空字符串转为 {@link Optional}。
     * <p>
     * 若字符串为 {@code null} 或空白则返回 {@link Optional#empty()}，
     * 否则返回包含该值的 Optional。
     *
     * @param value 原始字符串 / raw string value
     * @return Optional 包装结果 / optional wrapping the value if non-blank
     */
    private static Optional<String> optionalString(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    /**
     * 将可空字符串转为 {@link Optional<URI>}。
     * <p>
     * 先通过 {@link #optionalString(String)} 处理空白检查，
     * 再调用 {@link URI#create(String)} 解析 URI。
     *
     * @param value 原始字符串 / raw string value
     * @return Optional 包装的 URI / optional wrapping the URI if non-blank
     */
    private static Optional<URI> optionalUri(String value) {
        return optionalString(value).map(URI::create);
    }

    /**
     * 命令行参数解析记录。
     * <p>
     * 将 {@code String[] args} 解析为三部分：
     * <ul>
     *   <li>{@code positionals} —— 不以 {@code --} 开头的裸参数</li>
     *   <li>{@code options} —— {@code --key value} 形式的键值选项</li>
     *   <li>{@code flags} —— {@code --flag} 形式的布尔标志</li>
     * </ul>
     *
     * @param positionals 位置参数列表 / positional arguments
     * @param options     键值选项映射 / key-value options
     * @param flags       布尔标志列表 / boolean flags
     */
    private record CliArgs(List<String> positionals, Map<String, String> options, List<String> flags) {

        /**
         * 解析原始命令行参数数组。
         * <p>
         * 遍历 {@code args}，以 {@code --} 为界区分位置参数与选项；
         * 选项值紧跟在同名参数后且不以 {@code --} 开头时才能被正确解析。
         *
         * @param args 原始命令行参数 / raw command line arguments
         * @return 解析后的 {@link CliArgs} 实例 / parsed CliArgs instance
         */
        static CliArgs parse(String[] args) {
            List<String> positionals = new ArrayList<>();
            Map<String, String> options = new HashMap<>();
            List<String> flags = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (!arg.startsWith("--")) {
                    positionals.add(arg);
                    continue;
                }
                String name = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    options.put(name, args[++i]);
                } else {
                    flags.add(name);
                }
            }
            return new CliArgs(positionals, options, flags);
        }

        /**
         * 判断指定名称的标志是否存在。 / Check whether a flag is present.
         *
         * @param name 标志名（不含 {@code --} 前缀） / flag name without leading {@code --}
         * @return true 若该标志已设置 / true if the flag is set
         */
        boolean hasFlag(String name) {
            return flags.contains(name);
        }

        /**
         * 获取指定选项的值。 / Get the value of an option.
         *
         * @param name 选项名（不含 {@code --} 前缀） / option name without leading {@code --}
         * @return 选项值，若未提供则返回 {@code null} / option value, or {@code null} if not provided
         */
        String option(String name) {
            return options.get(name);
        }

        /**
         * 获取指定选项的值，若缺失则返回默认值。 / Get the option value or a default if absent.
         *
         * @param name         选项名 / option name
         * @param defaultValue 默认值 / default value
         * @return 选项值或默认值 / option value or default
         */
        String optionOrDefault(String name, String defaultValue) {
            return options.getOrDefault(name, defaultValue);
        }
    }
}
