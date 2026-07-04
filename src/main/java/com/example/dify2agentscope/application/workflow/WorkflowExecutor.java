package com.example.dify2agentscope.application.workflow;

import com.example.dify2agentscope.domain.dify.AgentSpec;
import com.example.dify2agentscope.domain.dify.DifyEdge;
import com.example.dify2agentscope.domain.dify.DifyNode;
import com.example.dify2agentscope.domain.dify.WorkflowPlan;
import com.example.dify2agentscope.domain.integration.AgentInvoker;
import com.example.dify2agentscope.domain.integration.KnowledgeRetriever;
import com.example.dify2agentscope.domain.memory.MemoRecord;
import com.example.dify2agentscope.domain.memory.MemoSearchRequest;
import com.example.dify2agentscope.domain.memory.MemoStore;
import com.example.dify2agentscope.domain.security.OutputSanitizer;
import com.example.dify2agentscope.domain.security.PermissionPolicy;
import com.example.dify2agentscope.domain.trace.ExecutionTracer;
import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import com.example.dify2agentscope.domain.workflow.WorkflowNodeTimeoutException;
import com.example.dify2agentscope.domain.workflow.WorkflowRequest;
import com.example.dify2agentscope.domain.workflow.WorkflowResponse;
import com.example.dify2agentscope.domain.workflow.WorkflowRuntime;
import com.example.dify2agentscope.infrastructure.trace.LoggingExecutionTracer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 执行当前服务支持的 Dify 工作流语义。
 * Executes the Dify workflow semantics supported by the current service.
 * 未支持的节点类型会直接失败，避免新导入的 DSL 在能力缺失时静默执行出不完整结果。
 * Unsupported node types fail immediately to prevent silently producing incomplete results
 * when a newly imported DSL uses capabilities that are not available.
 */
public class WorkflowExecutor {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{#([^#]+)#}}");
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F300}-\\x{1FAFF}\\u2600-\\u27BF]");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[!?。,，。!?~～\\. …]+");
    private static final Pattern SPACE_PATTERN = Pattern.compile("[\\s\\u3000]+");
    private static final Pattern FILLER_PATTERN = Pattern.compile("[嗯啊哦呢呀吧哈哎嘿呐]");

    private final String workflowId;
    private final WorkflowPlan plan;
    private final WorkflowRuntime runtime;
    private final AgentInvoker agentInvoker;
    private final KnowledgeRetriever knowledgeRetriever;
    private final MemoStore memoStore;
    private final PermissionPolicy permissionPolicy;
    private final OutputSanitizer outputSanitizer;
    private final ExecutionTracer tracer;
    private final int maxSteps;
    private final Duration defaultNodeTimeout;
    private final Map<String, Duration> nodeTimeouts;
    private final String fallbackAnswer;
    private final Pattern demoItemPattern;
    private final Pattern demoRequestPattern;
    private final Pattern demoTaskPattern;
    private final Map<String, AgentSpec> agentsByNodeId;
    private final String startNodeId;

    /**
     * 使用默认参数创建工作流执行器。
     * Creates a workflow executor with default parameters.
     *
     * @param plan               工作流计划 / workflow plan
     * @param agentInvoker       Agent 调用器 / agent invoker
     * @param knowledgeRetriever 知识检索器 / knowledge retriever
     * @param permissionPolicy   权限策略 / permission policy
     * @param outputSanitizer    输出消毒器 / output sanitizer
     */
    public WorkflowExecutor(
            WorkflowPlan plan,
            AgentInvoker agentInvoker,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer) {
        this("default",
                plan,
                agentInvoker,
                knowledgeRetriever,
                new com.example.dify2agentscope.infrastructure.memory.NoOpMemoStore(),
                permissionPolicy,
                outputSanitizer,
                new LoggingExecutionTracer(),
                100,
                Duration.ofSeconds(30),
                Map.of(),
                "当前系统繁忙，无法联网工作，请稍后重试！",
                "ITEM-\\d{4}",
                "REQ-\\d{4}",
                "TASK-\\d{4}");
    }

    /**
     * 全参构造器，允许注入所有依赖与配置。
     * Full-argument constructor allowing injection of all dependencies and configuration.
     *
     * @param workflowId         工作流标识 / workflow identifier
     * @param plan               工作流计划 / workflow plan
     * @param agentInvoker       Agent 调用器 / agent invoker
     * @param knowledgeRetriever 知识检索器 / knowledge retriever
     * @param permissionPolicy   权限策略 / permission policy
     * @param outputSanitizer    输出消毒器 / output sanitizer
     * @param tracer             执行追踪器 / execution tracer
     * @param maxSteps           最大执行步数 / max execution steps
     * @param defaultNodeTimeout 默认节点超时时长 / default node timeout
     * @param nodeTimeouts       节点 ID 或类型的超时覆盖 / timeout overrides by node id or type
     * @param fallbackAnswer     兜底回复 / fallback answer
     * @param demoItemPattern    demo item 正则 / demo item regex
     * @param demoRequestPattern demo request 正则 / demo request regex
     * @param demoTaskPattern    demo task 正则 / demo task regex
     */
    public WorkflowExecutor(
            String workflowId,
            WorkflowPlan plan,
            AgentInvoker agentInvoker,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer tracer,
            int maxSteps,
            String fallbackAnswer,
            String demoItemPattern,
            String demoRequestPattern,
            String demoTaskPattern) {
        this(workflowId,
                plan,
                agentInvoker,
                knowledgeRetriever,
                new com.example.dify2agentscope.infrastructure.memory.NoOpMemoStore(),
                permissionPolicy,
                outputSanitizer,
                tracer,
                maxSteps,
                Duration.ofSeconds(30),
                Map.of(),
                fallbackAnswer,
                demoItemPattern,
                demoRequestPattern,
                demoTaskPattern);
    }

    /**
     * 带节点超时配置的兼容构造器，默认使用空 Memos 存储。
     *
     * @param workflowId         工作流标识
     * @param plan               工作流计划
     * @param agentInvoker       Agent 调用器
     * @param knowledgeRetriever 知识检索器
     * @param permissionPolicy   权限策略
     * @param outputSanitizer    输出净化器
     * @param tracer             执行追踪器
     * @param maxSteps           最大执行步骤
     * @param defaultNodeTimeout 默认节点超时
     * @param nodeTimeouts       节点超时覆盖
     * @param fallbackAnswer     兜底回复
     * @param demoItemPattern    demo item 正则
     * @param demoRequestPattern demo request 正则
     * @param demoTaskPattern    demo task 正则
     */
    public WorkflowExecutor(
            String workflowId,
            WorkflowPlan plan,
            AgentInvoker agentInvoker,
            KnowledgeRetriever knowledgeRetriever,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer tracer,
            int maxSteps,
            Duration defaultNodeTimeout,
            Map<String, Duration> nodeTimeouts,
            String fallbackAnswer,
            String demoItemPattern,
            String demoRequestPattern,
            String demoTaskPattern) {
        this(workflowId,
                plan,
                agentInvoker,
                knowledgeRetriever,
                new com.example.dify2agentscope.infrastructure.memory.NoOpMemoStore(),
                permissionPolicy,
                outputSanitizer,
                tracer,
                maxSteps,
                defaultNodeTimeout,
                nodeTimeouts,
                fallbackAnswer,
                demoItemPattern,
                demoRequestPattern,
                demoTaskPattern);
    }

    public WorkflowExecutor(
            String workflowId,
            WorkflowPlan plan,
            AgentInvoker agentInvoker,
            KnowledgeRetriever knowledgeRetriever,
            MemoStore memoStore,
            PermissionPolicy permissionPolicy,
            OutputSanitizer outputSanitizer,
            ExecutionTracer tracer,
            int maxSteps,
            Duration defaultNodeTimeout,
            Map<String, Duration> nodeTimeouts,
            String fallbackAnswer,
            String demoItemPattern,
            String demoRequestPattern,
            String demoTaskPattern) {
        this.workflowId = workflowId;
        this.plan = plan;
        this.runtime = new WorkflowRuntime(plan);
        this.agentInvoker = agentInvoker;
        this.knowledgeRetriever = knowledgeRetriever;
        this.memoStore = memoStore;
        this.permissionPolicy = permissionPolicy;
        this.outputSanitizer = outputSanitizer;
        this.tracer = tracer;
        this.maxSteps = maxSteps;
        this.defaultNodeTimeout = defaultNodeTimeout == null ? Duration.ZERO : defaultNodeTimeout;
        this.nodeTimeouts = nodeTimeouts == null ? Map.of() : Map.copyOf(nodeTimeouts);
        this.fallbackAnswer = fallbackAnswer;
        this.demoItemPattern = Pattern.compile(demoItemPattern);
        this.demoRequestPattern = Pattern.compile(demoRequestPattern);
        this.demoTaskPattern = Pattern.compile(demoTaskPattern);
        this.agentsByNodeId = plan.agents().stream()
                .collect(Collectors.toMap(AgentSpec::nodeId, agent -> agent));
        String detectedStartNodeId = plan.nodes().stream()
                .filter(node -> "start".equals(node.type()))
                .findFirst()
                .map(DifyNode::id)
                .orElse(null);
        if (detectedStartNodeId == null && !isReactSelfLoopMode(plan) && !isHarnessMode(plan)) {
            throw new IllegalArgumentException("DAG workflow has no start node");
        }
        this.startNodeId = detectedStartNodeId;
    }

    /**
     * 将配置值解析为整数。
     *
     * @param value        原始配置值
     * @param defaultValue 解析失败时使用的默认值
     * @return 解析后的整数
     */
    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行整个工作流，返回最终回复或兜底错误信息。
     * Executes the entire workflow and returns the final answer or a fallback error message.
     *
     * @param request 工作流请求 / workflow request
     * @return 工作流响应 / workflow response
     */
    public WorkflowResponse execute(WorkflowRequest request) {
        WorkflowExecutionContext context = new WorkflowExecutionContext(workflowId, plan, request);
        Instant workflowStart = Instant.now();
        tracer.workflowStarted(context);
        try {
            permissionPolicy.validateRequest(request);
            if (isHarnessMode()) {
                throw new UnsupportedOperationException(
                        "agentscope-harness requires official Harness/Multi-Agent integration and is not enabled yet");
            }
            String answer = isReactSelfLoopMode() ? runReactSelfLoop(context) : runGraph(context);
            tracer.workflowFinished(context, true, Duration.between(workflowStart, Instant.now()), Map.of());
            return new WorkflowResponse(
                    outputSanitizer.sanitize(answer),
                    new LinkedHashMap<>(context.conversation()),
                    context.nodeOutputs(),
                    context.executionPath(),
                    context.traceId(),
                    true,
                    "");
        } catch (Exception e) {
            tracer.workflowFinished(
                    context,
                    false,
                    Duration.between(workflowStart, Instant.now()),
                    Map.of("error", e.getMessage()));
            return new WorkflowResponse(
                    fallbackAnswer,
                    new LinkedHashMap<>(context.conversation()),
                    context.nodeOutputs(),
                    context.executionPath(),
                    context.traceId(),
                    false,
                    e.getMessage());
        }
    }

    /**
     * 按 DAG 顺序遍历节点，到达 answer 节点后返回结果。
     * Traverses nodes in DAG order and returns the result when an answer node is reached.
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @return 最终回答文本 / final answer text
     */
    private String runGraph(WorkflowExecutionContext context) {
        String current = startNodeId;
        // 步数上限用于防止异常 DSL 环路拖垮生产实例。
        for (int step = 0; step < maxSteps && current != null && !current.isBlank(); step++) {
            DifyNode node = runtime.node(current);
            if (node == null) {
                throw new IllegalStateException("Missing node: " + current);
            }
            context.executionPath().add(current);
            Instant nodeStart = Instant.now();
            tracer.nodeStarted(context, node);
            String handle;
            try {
                handle = executeNodeWithTimeout(node, context);
                Duration duration = Duration.between(nodeStart, Instant.now());
                recordNodeSuccess(context, node, duration);
                tracer.nodeFinished(context, node, handle, duration);
            } catch (Exception e) {
                Duration duration = Duration.between(nodeStart, Instant.now());
                recordNodeFailure(context, node, duration, e);
                tracer.nodeFailed(context, node, duration, e);
                throw e;
            }
            if ("answer".equals(node.type())) {
                return stringValue(context.nodeOutputs().get(node.id()).get("answer"));
            }
            current = nextNode(current, handle);
        }
        throw new IllegalStateException("Workflow did not reach answer node");
    }

    /**
     * 执行 AgentScope 自循环模式。
     * <p>该模式不再依赖外部 DAG 边，而是把请求交给第一个 ReActAgent/HarnessAgent 节点自行完成
     * reasoning/acting。当前是最小实现，用于从 Dify DAG 迁移到 AgentScope 官方自循环能力。</p>
     *
     * @param context 工作流执行上下文
     * @return Agent 最终回复文本
     */
    private String runReactSelfLoop(WorkflowExecutionContext context) {
        AgentSpec agent = plan.agents().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("React self-loop workflow must contain an agent"));
        DifyNode node = runtime.node(agent.nodeId());
        if (node == null) {
            node = new DifyNode(agent.nodeId(), "agent", agent.title(), Map.of("type", "agent"));
        }
        context.executionPath().add(node.id());
        Instant nodeStart = Instant.now();
        tracer.nodeStarted(context, node);
        try {
            prepareReactAgentContext(node, context);
            String handle = executeNodeWithTimeout(node, context);
            Duration duration = Duration.between(nodeStart, Instant.now());
            recordNodeSuccess(context, node, duration);
            tracer.nodeFinished(context, node, handle, duration);
            return stringValue(context.nodeOutputs().get(node.id()).get("text"));
        } catch (Exception e) {
            Duration duration = Duration.between(nodeStart, Instant.now());
            recordNodeFailure(context, node, duration, e);
            tracer.nodeFailed(context, node, duration, e);
            throw e;
        }
    }

    /**
     * 为 ReAct 自循环 Agent 准备项目级上下文。
     * <p>官方 ReAct 执行不走外部 DAG；因此 Memos 和 Knowledge 这类项目中间件在 Agent 调用前注入
     * conversation，随后由 AgentScope 官方 ReActAgent 通过 prompt/context 使用。</p>
     *
     * @param node    Agent 节点
     * @param context 工作流执行上下文
     */
    private void prepareReactAgentContext(DifyNode node, WorkflowExecutionContext context) {
        Map<String, Object> data = data(node);
        if (data.containsKey("memo-limit")) {
            Map<String, Object> memos = retrieveMemos(Map.of("limit", data.get("memo-limit")), context);
            context.conversation().put("_agent_memos", memos.get("result"));
            context.conversation().put("_agent_memo_records", memos.get("memos"));
        }
        if (data.get("knowledge") instanceof Map<?, ?> rawKnowledge) {
            Map<String, Object> knowledgeSettings = new LinkedHashMap<>();
            rawKnowledge.forEach((key, value) -> knowledgeSettings.put(String.valueOf(key), value));
            knowledgeSettings.putIfAbsent("type", "knowledge-retrieval");
            knowledgeSettings.putIfAbsent("title", node.title() + " Knowledge");
            DifyNode knowledgeNode = new DifyNode(
                    node.id() + ":knowledge",
                    "knowledge-retrieval",
                    node.title() + " Knowledge",
                    knowledgeSettings);
            Map<String, Object> knowledge = knowledgeRetriever.retrieve(knowledgeNode, context);
            context.conversation().put("_agent_knowledge", knowledge);
        }
    }

    /**
     * 判断当前 workflow 是否使用 AgentScope 自循环执行模式。
     *
     * @return true 表示使用 ReAct 自循环，不执行外部 DAG
     */
    private boolean isReactSelfLoopMode() {
        return isReactSelfLoopMode(plan);
    }

    /**
     * 判断指定计划是否使用 AgentScope 自循环执行模式。
     *
     * @param plan workflow 计划
     * @return true 表示使用 ReAct 自循环，不执行外部 DAG
     */
    private static boolean isReactSelfLoopMode(WorkflowPlan plan) {
        return "agentscope-react".equalsIgnoreCase(plan.mode())
                || "react-self-loop".equalsIgnoreCase(plan.mode());
    }

    /**
     * 判断当前 workflow 是否声明为 Harness/Multi-Agent 模式。
     * <p>官方 Harness 尚未接入前必须显式失败，避免把 ReAct 最小实现误当作官方多 Agent 编排。</p>
     *
     * @return true 表示当前计划要求 Harness/Multi-Agent 执行
     */
    private boolean isHarnessMode() {
        return isHarnessMode(plan);
    }

    /**
     * 判断指定计划是否声明为 Harness/Multi-Agent 模式。
     *
     * @param plan workflow 计划
     * @return true 表示当前计划要求 Harness/Multi-Agent 执行
     */
    private static boolean isHarnessMode(WorkflowPlan plan) {
        return "agentscope-harness".equalsIgnoreCase(plan.mode());
    }

    /**
     * 根据节点类型分发执行逻辑。
     * Dispatches execution logic based on the node type.
     *
     * @param node    当前节点 / current node
     * @param context 工作流执行上下文 / workflow execution context
     * @return 输出句柄，用于决定下一条边的选择 / output handle for next edge selection
     */
    @SuppressWarnings("unchecked")
    private String executeNode(DifyNode node, WorkflowExecutionContext context) {
        Map<String, Object> data = data(node);
        return switch (node.type()) {
            case "start" -> {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("x_dify_chat_id", context.request().xDifyChatId());
                output.put("x_menu_id", context.request().xMenuId());
                context.putNodeOutput(node.id(), output);
                yield "source";
            }
            case "code" -> {
                context.putNodeOutput(node.id(), executeCodeNode(node, context));
                yield "source";
            }
            case "if-else" -> {
                String matched = evaluateIfElse(data, context);
                context.putNodeOutput(node.id(), Map.of("result", matched));
                yield matched;
            }
            case "assigner" -> {
                executeAssigner(data, context);
                context.putNodeOutput(node.id(), Map.of("ok", true));
                yield "source";
            }
            case "knowledge-retrieval" -> {
                context.putNodeOutput(node.id(), knowledgeRetriever.retrieve(node, context));
                yield "source";
            }
            case "memo-retrieval" -> {
                context.putNodeOutput(node.id(), retrieveMemos(data, context));
                yield "source";
            }
            case "agent" -> {
                AgentSpec agent = agentsByNodeId.get(node.id());
                if (agent == null) {
                    throw new IllegalStateException("Missing agent spec for node: " + node.id());
                }
                String text = agentInvoker.invoke(agent, context);
                context.putNodeOutput(node.id(), Map.of("text", text));
                yield "source";
            }
            case "answer" -> {
                String answer = renderTemplate(stringValue(data.get("answer")), context);
                context.putNodeOutput(node.id(), Map.of("answer", answer));
                yield "source";
            }
            default -> throw new IllegalStateException("Unsupported node type: " + node.type());
        };
    }

    /**
     * 按节点配置的最大处理时间执行节点，超时则默认失败。
     *
     * @param node    当前节点
     * @param context 工作流执行上下文
     * @return 输出句柄
     */
    private String executeNodeWithTimeout(DifyNode node, WorkflowExecutionContext context) {
        Duration timeout = nodeTimeout(node);
        if (timeout.isZero() || timeout.isNegative()) {
            return executeNode(node, context);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "workflow-node-" + workflowId + "-" + node.id());
            thread.setDaemon(true);
            return thread;
        });
        Future<String> future = executor.submit(() -> executeNode(node, context));
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new WorkflowNodeTimeoutException(node.id(), timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Node execution interrupted: " + node.id(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Node execution failed: " + node.id(), cause);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 解析当前节点的超时配置，优先级为节点 data、节点 ID 覆盖、节点类型覆盖、全局默认值。
     *
     * @param node 当前节点
     * @return 节点超时时长
     */
    private Duration nodeTimeout(DifyNode node) {
        Map<String, Object> data = data(node);
        Duration inlineTimeout = parseDuration(data.get("timeout-ms"))
                .or(() -> parseDuration(data.get("timeoutMs")))
                .or(() -> parseDuration(data.get("timeout")))
                .orElse(null);
        if (inlineTimeout != null) {
            return inlineTimeout;
        }
        Duration byNodeId = nodeTimeouts.get(node.id());
        if (byNodeId != null) {
            return byNodeId;
        }
        Duration byType = nodeTimeouts.get(node.type());
        if (byType != null) {
            return byType;
        }
        return defaultNodeTimeout;
    }

    /**
     * 将节点成功状态和耗时追加到节点输出中。
     *
     * @param context  执行上下文
     * @param node     当前节点
     * @param duration 节点实际耗时
     */
    private void recordNodeSuccess(WorkflowExecutionContext context, DifyNode node, Duration duration) {
        Map<String, Object> output = new LinkedHashMap<>(
                context.nodeOutputs().getOrDefault(node.id(), Map.of()));
        output.put("_status", "success");
        output.put("_duration_ms", duration.toMillis());
        context.putNodeOutput(node.id(), output);
    }

    /**
     * 将节点失败状态、耗时和异常详情写入节点输出。
     *
     * @param context  执行上下文
     * @param node     失败节点
     * @param duration 节点实际耗时
     * @param error    节点异常
     */
    private void recordNodeFailure(
            WorkflowExecutionContext context,
            DifyNode node,
            Duration duration,
            Exception error) {
        Map<String, Object> output = new LinkedHashMap<>(
                context.nodeOutputs().getOrDefault(node.id(), Map.of()));
        output.put("_status", "failed");
        output.put("_duration_ms", duration.toMillis());
        output.put("_error", errorPayload(node, duration, error));
        context.putNodeOutput(node.id(), output);
    }

    /**
     * 构造节点异常详情，供 API 响应和日志排障使用。
     *
     * @param node     出错节点
     * @param duration 节点耗时
     * @param error    异常对象
     * @return 结构化异常信息
     */
    private Map<String, Object> errorPayload(DifyNode node, Duration duration, Exception error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nodeId", node.id());
        payload.put("nodeType", node.type());
        payload.put("nodeTitle", node.title());
        payload.put("durationMs", duration.toMillis());
        payload.put("timeoutMs", nodeTimeout(node).toMillis());
        payload.put("errorType", error.getClass().getSimpleName());
        payload.put("message", error.getMessage());
        return payload;
    }

    /**
     * 将节点配置中的毫秒数或 ISO-8601 Duration 字符串解析为时长。
     *
     * @param value 配置值
     * @return 解析成功后的时长
     */
    private Optional<Duration> parseDuration(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Duration duration) {
            return Optional.of(duration);
        }
        if (value instanceof Number number) {
            return Optional.of(Duration.ofMillis(number.longValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.ofMillis(Long.parseLong(text)));
        } catch (NumberFormatException ignored) {
            return Optional.of(Duration.parse(text));
        }
    }

    /**
     * 执行 Memos 长期记忆检索节点。
     *
     * @param data    节点配置
     * @param context 工作流执行上下文
     * @return 包含 Markdown 结果和原始记录列表的节点输出
     */
    private Map<String, Object> retrieveMemos(Map<String, Object> data, WorkflowExecutionContext context) {
        int limit = parseInt(data.get("limit"), 5);
        List<MemoRecord> records = memoStore.search(new MemoSearchRequest(
                context.request().userId(),
                context.request().query(),
                limit));
        String result = records.stream()
                .map(record -> "- " + record.content())
                .collect(Collectors.joining("\n"));
        return Map.of(
                "result", result,
                "memos", records);
    }

    /**
     * 根据当前节点和输出句柄查找下一个节点。
     * Finds the next node based on the current node and output handle.
     *
     * @param current 当前节点 ID / current node ID
     * @param handle  输出句柄 / output handle
     * @return 下一节点 ID，无可达节点时返回 null / next node ID or null if unreachable
     */
    private String nextNode(String current, String handle) {
        List<DifyEdge> edges = runtime.nextEdges(current);
        Optional<DifyEdge> exact = edges.stream()
                .filter(edge -> handle.equals(edge.sourceHandle()))
                .findFirst();
        if (exact.isPresent()) {
            return exact.get().target();
        }
        return edges.stream()
                .filter(edge -> "source".equals(edge.sourceHandle()))
                .findFirst()
                .map(DifyEdge::target)
                .orElse(null);
    }

    /**
     * 执行代码节点，根据输出字段定义提取标识符或生成时间。
     * Executes a code node, extracting identifiers or generating timestamps based on output field definitions.
     *
     * @param node    代码节点 / code node
     * @param context 工作流执行上下文 / workflow execution context
     * @return 节点输出 / node output
     */
    private Map<String, Object> executeCodeNode(DifyNode node, WorkflowExecutionContext context) {
        Map<String, Object> data = data(node);
        Object outputs = data.get("outputs");
        if (outputs instanceof Map<?, ?> map && isIdentifierExtractionOutput(map)) {
            return extractIdentifiers(context);
        }
        if (outputs instanceof Map<?, ?> map && map.containsKey("current_time")) {
            return Map.of("current_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return Map.of();
    }

    /**
     * 判断 code 节点输出是否表示 demo 标识符提取。
     * <p>优先识别无业务含义的 demo 字段；旧字段名继续兼容历史 DSL。</p>
     *
     * @param outputs code 节点输出定义
     * @return true 表示需要执行标识符提取
     */
    private boolean isIdentifierExtractionOutput(Map<?, ?> outputs) {
        boolean demoOutputs = outputs.containsKey("demo_item_id")
                && outputs.containsKey("demo_request_id")
                && outputs.containsKey("demo_task_id");
        boolean legacyOutputs = outputs.containsKey("waybillNumber")
                && outputs.containsKey("order_code")
                && outputs.containsKey("work_code");
        return demoOutputs || legacyOutputs;
    }

    /**
     * 从用户查询中提取三类无业务含义的 demo 标识符。
     * <p>字段名以 {@code demo_*} 为主，旧的业务字段名仅作为兼容别名输出。</p>
     *
     * @param context 工作流执行上下文 / workflow execution context
     * @return 包含提取结果的 Map / map containing extracted identifiers
     */
    private Map<String, Object> extractIdentifiers(WorkflowExecutionContext context) {
        String query = context.request().query();
        String cleaned = cleanText(query);
        List<String> demoItems = distinctMatches(demoItemPattern, cleaned, 200);
        List<String> demoRequests = distinctMatches(demoRequestPattern, cleaned, 10);
        List<String> demoTasks = distinctMatches(demoTaskPattern, cleaned, 1);

        String demoItem = demoItems.isEmpty() ? "" : demoItems.get(0);
        String demoRequest = demoRequests.isEmpty() ? "" : demoRequests.get(0);
        String demoTask = demoTasks.isEmpty() ? "" : demoTasks.get(0);

        if (demoItem.isBlank() && demoRequest.isBlank() && demoTask.isBlank()) {
            HistoryIdentifiers history = latestFromHistory(context.conversation().get("history_user_query"));
            demoItem = history.demoItem();
            demoItems = history.demoItems();
            demoRequest = history.demoRequest();
            demoTask = history.demoTask();
        }

        String historyChat = "";
        Object chatRound = context.conversation().get("chat_round");
        if (chatRound != null && query != null && !query.isBlank()) {
            historyChat = "第" + chatRound + "轮 | 问题：" + query + " | 回复："
                    + stringValue(context.conversation().get("reply"));
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("demo_item_id", demoItem);
        output.put("demo_item_id_list", demoItems);
        output.put("demo_request_id", demoRequest);
        output.put("demo_task_id", demoTask);
        output.put("waybillNumber", demoItem);
        output.put("waybillNumberList", demoItems);
        output.put("order_code", demoRequest);
        output.put("work_code", demoTask);
        output.put("history_chat", historyChat);
        return output;
    }

    /**
     * 从对话历史中获取最近非空的标识符。
     * Retrieves the latest non-empty identifiers from conversation history.
     *
     * @param historyValue 历史值 / history value
     * @return 历史标识符记录 / history identifiers record
     */
    private HistoryIdentifiers latestFromHistory(Object historyValue) {
        if (!(historyValue instanceof List<?> history) || history.isEmpty()) {
            return new HistoryIdentifiers("", List.of(), "", "");
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Object item = history.get(i);
            HistoryIdentifiers found = identifiersFromHistoryItem(item);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return new HistoryIdentifiers("", List.of(), "", "");
    }

    /**
     * 从单条历史条目中提取标识符（支持 Map 和 String 两种格式）。
     * Extracts identifiers from a single history item, supporting both Map and String formats.
     *
     * @param item 历史条目 / history item
     * @return 解析后的标识符 / parsed identifiers
     */
    @SuppressWarnings("unchecked")
    private HistoryIdentifiers identifiersFromHistoryItem(Object item) {
        if (item instanceof Map<?, ?> map) {
            String demoItem = firstNonBlank(map.get("demo_item_id"), map.get("waybillNumber"));
            Object listValue = map.containsKey("demo_item_id_list")
                    ? map.get("demo_item_id_list")
                    : map.get("waybillNumberList");
            List<String> demoItems = listValue instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : demoItem.isBlank() ? List.of() : List.of(demoItem);
            return new HistoryIdentifiers(
                    demoItem,
                    demoItems,
                    firstNonBlank(map.get("demo_request_id"), map.get("order_code")),
                    firstNonBlank(map.get("demo_task_id"), map.get("work_code")));
        }
        if (item instanceof String text) {
            String cleaned = cleanText(text);
            List<String> demoItems = distinctMatches(demoItemPattern, cleaned, 200);
            List<String> demoRequests = distinctMatches(demoRequestPattern, cleaned, 10);
            List<String> demoTasks = distinctMatches(demoTaskPattern, cleaned, 1);
            return new HistoryIdentifiers(
                    demoItems.isEmpty() ? "" : demoItems.get(0),
                    demoItems,
                    demoRequests.isEmpty() ? "" : demoRequests.get(0),
                    demoTasks.isEmpty() ? "" : demoTasks.get(0));
        }
        return new HistoryIdentifiers("", List.of(), "", "");
    }

    /**
     * 返回第一个非空字符串。
     *
     * @param values 候选值
     * @return 第一个非空字符串，不存在时返回空字符串
     */
    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    /**
     * 评估 if-else 节点的条件分支，返回匹配的 case_id。
     * Evaluates if-else node condition branches and returns the matched case_id.
     *
     * @param data    节点数据 / node data
     * @param context 工作流执行上下文 / workflow execution context
     * @return 匹配的 case_id，无匹配时返回 "false" / matched case_id, or "false" if none
     */
    @SuppressWarnings("unchecked")
    private String evaluateIfElse(Map<String, Object> data, WorkflowExecutionContext context) {
        Object casesValue = data.get("cases");
        if (!(casesValue instanceof List<?> cases)) {
            return "false";
        }
        for (Object rawCase : cases) {
            Map<String, Object> caseMap = (Map<String, Object>) rawCase;
            if (evaluateCase(caseMap, context)) {
                return stringValue(caseMap.get("case_id"));
            }
        }
        return "false";
    }

    /**
     * 评估单个 case 中的所有条件（AND/OR 逻辑）。
     * Evaluates all conditions in a single case using AND/OR logic.
     *
     * @param caseMap case 配置 / case configuration
     * @param context 工作流执行上下文 / workflow execution context
     * @return 条件是否满足 / whether the conditions are satisfied
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateCase(Map<String, Object> caseMap, WorkflowExecutionContext context) {
        String logical = stringValue(caseMap.getOrDefault("logical_operator", "and"));
        List<?> conditions = (List<?>) caseMap.getOrDefault("conditions", List.of());
        boolean result = !"or".equals(logical);
        for (Object rawCondition : conditions) {
            boolean matched = evaluateCondition((Map<String, Object>) rawCondition, context);
            if ("or".equals(logical)) {
                result |= matched;
            } else {
                result &= matched;
            }
        }
        return result;
    }

    /**
     * 评估单条条件：根据比较运算符检查变量值。
     * Evaluates a single condition by comparing a variable value using the specified operator.
     *
     * @param condition 条件配置 / condition configuration
     * @param context   工作流执行上下文 / workflow execution context
     * @return 条件是否成立 / whether the condition holds
     */
    private boolean evaluateCondition(Map<String, Object> condition, WorkflowExecutionContext context) {
        Object value = context.resolve((List<?>) condition.get("variable_selector"));
        String actual = stringValue(value);
        String expected = stringValue(condition.get("value"));
        return switch (stringValue(condition.get("comparison_operator"))) {
            case "not empty" -> !isEmpty(value);
            case "empty" -> isEmpty(value);
            case "is" -> actual.equals(expected);
            default -> false;
        };
    }

    /**
     * 执行变量赋值器节点，更新会话上下文。
     * Executes an assigner node to update the conversation context.
     *
     * @param data    节点数据 / node data
     * @param context 工作流执行上下文 / workflow execution context
     */
    @SuppressWarnings("unchecked")
    private void executeAssigner(Map<String, Object> data, WorkflowExecutionContext context) {
        Object itemsValue = data.get("items");
        if (!(itemsValue instanceof List<?> items)) {
            return;
        }
        for (Object rawItem : items) {
            Map<String, Object> item = (Map<String, Object>) rawItem;
            List<?> variableSelector = (List<?>) item.get("variable_selector");
            if (variableSelector == null || variableSelector.size() < 2 || !"conversation".equals(variableSelector.get(0))) {
                continue;
            }
            String target = String.valueOf(variableSelector.get(1));
            Object value = "constant".equals(item.get("input_type"))
                    ? item.get("value")
                    : context.resolve((List<?>) item.get("value"));
            String operation = stringValue(item.get("operation"));
            if ("append".equals(operation)) {
                appendConversation(context.conversation(), target, value);
            } else if ("+=".equals(operation)) {
                incrementConversation(context.conversation(), target, value);
            } else {
                context.conversation().put(target, value);
            }
        }
    }

    /**
     * 向会话中的列表字段追加值（若不存在则创建列表）。
     * Appends a value to a list field in the conversation, creating the list if it does not exist.
     *
     * @param conversation 会话上下文 / conversation context
     * @param target       目标字段名 / target field name
     * @param value        待追加的值 / value to append
     */
    @SuppressWarnings("unchecked")
    private void appendConversation(Map<String, Object> conversation, String target, Object value) {
        Object existing = conversation.get(target);
        List<Object> values;
        if (existing instanceof List<?> list) {
            values = new ArrayList<>((List<Object>) list);
        } else {
            values = new ArrayList<>();
            if (existing != null && !stringValue(existing).isBlank()) {
                values.add(existing);
            }
        }
        if (value != null && !stringValue(value).isBlank()) {
            values.add(value);
        }
        conversation.put(target, values);
    }

    /**
     * 对会话中的数字字段执行累加操作。
     * Performs an increment operation on a numeric field in the conversation.
     *
     * @param conversation 会话上下文 / conversation context
     * @param target       目标字段名 / target field name
     * @param value        累加值 / increment value
     */
    private void incrementConversation(Map<String, Object> conversation, String target, Object value) {
        long current = 0L;
        Object existing = conversation.get(target);
        if (existing instanceof Number number) {
            current = number.longValue();
        } else if (existing != null && !stringValue(existing).isBlank()) {
            current = Long.parseLong(stringValue(existing));
        }
        long delta = value instanceof Number number ? number.longValue() : Long.parseLong(stringValue(value));
        conversation.put(target, current + delta);
    }

    /**
     * 渲染模板字符串，将 {{#key.subKey#}} 替换为上下文中的变量值。
     * Renders a template string by replacing {{#key.subKey#}} with the corresponding context variable.
     *
     * @param template 模板字符串 / template string
     * @param context  工作流执行上下文 / workflow execution context
     * @return 渲染后的字符串 / rendered string
     */
    private String renderTemplate(String template, WorkflowExecutionContext context) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String[] parts = matcher.group(1).split("\\.");
            Object value = parts.length >= 2 ? context.resolve(List.of(parts[0], parts[1])) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(stringValue(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 获取节点数据并转换为 Map。
     * Retrieves node data and casts it to Map.
     *
     * @param node Dify 节点 / Dify node
     * @return 节点数据 Map / node data as Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> data(DifyNode node) {
        return (Map<String, Object>) node.data();
    }

    /**
     * 清理文本：去除空白、表情符号、标点、填充词，并标准化 ky 前缀。
     * Cleans text by removing whitespace, emojis, punctuation, filler words, and normalizing the ky prefix.
     *
     * @param text 原始文本 / raw text
     * @return 清理后的文本 / cleaned text
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = SPACE_PATTERN.matcher(text).replaceAll("");
        cleaned = cleaned.replace("ky", "KY");
        cleaned = EMOJI_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = PUNCTUATION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = FILLER_PATTERN.matcher(cleaned).replaceAll("");
        return cleaned;
    }

    /**
     * 从文本中匹配并去重提取指定模式的值，不超过上限。
     * Extracts distinct matches of a given pattern from text, up to a limit.
     *
     * @param pattern 正则模式 / regex pattern
     * @param value   输入文本 / input text
     * @param limit   最大匹配数 / max match count
     * @return 去重后的匹配结果 / deduplicated match results
     */
    private List<String> distinctMatches(Pattern pattern, String value, int limit) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        while (matcher.find() && matches.size() < limit) {
            String match = matcher.group();
            if (!matches.contains(match)) {
                matches.add(match);
            }
        }
        return matches;
    }

    /**
     * 判断值是否为空（null、空白字符串或空列表）。
     * Checks if a value is empty: null, blank string, or empty list.
     *
     * @param value 待检查值 / value to check
     * @return 是否为空 / whether it is empty
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    /**
     * 将对象安全转换为字符串，null 时返回空字符串。
     * Safely converts an object to a string, returning empty string for null.
     *
     * @param value 待转换对象 / object to convert
     * @return 字符串值 / string value
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 从用户消息或对话历史中抽取的 demo 标识符。
     */
    private record HistoryIdentifiers(
            String demoItem,
            List<String> demoItems,
            String demoRequest,
            String demoTask) {
        /**
         * 判断是否没有抽取到任何历史标识符。
         *
         * @return true 表示所有标识符均为空
         */
        boolean isEmpty() {
            return demoItem.isBlank() && demoItems.isEmpty() && demoRequest.isBlank() && demoTask.isBlank();
        }
    }
}
