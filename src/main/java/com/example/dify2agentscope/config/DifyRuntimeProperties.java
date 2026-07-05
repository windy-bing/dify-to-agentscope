package com.example.dify2agentscope.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dify → AgentScope 运行时配置属性映射类。
 * <p>
 * 绑定 {@code dify.agentscope.*} 前缀的 Spring Boot 配置项，集中管理 Dify 工作流转换引擎的全部运行时参数，
 * 包括 Higress MCP 网关、知识库端点、请求超时、安全边界、执行策略以及 Nacos 动态配置等。映射到
 * {@link DifyRuntimeProperties} 后，通过 {@link com.example.dify2agentscope.config.WorkflowRuntimeConfig}
 * 注入各基础设施组件。
 *
 * <pre>
 * dify.agentscope:
 *   default-workflow-id: default
 *   generated-output-dir: target/generated-workflows
 *   build-agents: true
 *   higress-mcp-endpoint: http://localhost:8080
 *   ...
 * </pre>
 */
@ConfigurationProperties(prefix = "dify.agentscope")
public class DifyRuntimeProperties {

    private String defaultWorkflowId = "default";
    private String generatedOutputDir = "target/generated-workflows";
    private Map<String, WorkflowDefinition> workflows = defaultWorkflows();
    private boolean buildAgents;
    private boolean failOnMissingIntegrations;
    private URI higressMcpEndpoint;
    private String higressMcpBearerToken;
    private URI knowledgeEndpoint;
    private String knowledgeBearerToken;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Map<String, String> extraHeaders = new LinkedHashMap<>();
    private Execution execution = new Execution();
    private Memory memory = new Memory();
    private Nacos nacos = new Nacos();
    private Boundary boundary = new Boundary();
    private Deployment deployment = new Deployment();
    private Persistence persistence = new Persistence();
    private WorkflowStore workflowStore = new WorkflowStore();

    /**
     * 获取默认工作流 ID。 / Get the default workflow identifier.
     *
     * @return 默认工作流 ID / default workflow id
     */
    public String getDefaultWorkflowId() {
        return defaultWorkflowId;
    }

    /**
     * 设置默认工作流 ID。 / Set the default workflow identifier.
     *
     * @param defaultWorkflowId 默认工作流 ID / default workflow id
     */
    public void setDefaultWorkflowId(String defaultWorkflowId) {
        this.defaultWorkflowId = defaultWorkflowId;
    }

    /**
     * 获取生成的工作流代码输出目录。 / Get the output directory for generated workflow code.
     *
     * @return 输出目录路径 / output directory path
     */
    public String getGeneratedOutputDir() {
        return generatedOutputDir;
    }

    /**
     * 设置生成的工作流代码输出目录。 / Set the output directory for generated workflow code.
     *
     * @param generatedOutputDir 输出目录路径 / output directory path
     */
    public void setGeneratedOutputDir(String generatedOutputDir) {
        this.generatedOutputDir = generatedOutputDir;
    }

    /**
     * 获取工作流定义映射表。 / Get the workflow definition map.
     *
     * @return 工作流 ID → 定义映射 / workflow id → definition map
     */
    public Map<String, WorkflowDefinition> getWorkflows() {
        return workflows;
    }

    /**
     * 设置工作流定义映射表。 / Set the workflow definition map.
     *
     * @param workflows 工作流 ID → 定义映射 / workflow id → definition map
     */
    public void setWorkflows(Map<String, WorkflowDefinition> workflows) {
        this.workflows = workflows;
    }

    /**
     * 是否构建 AgentScope ReActAgent。 / Whether to build AgentScope ReActAgent instances.
     *
     * @return true 则构建真实 Agent / true if real agents should be built
     */
    public boolean isBuildAgents() {
        return buildAgents;
    }

    /**
     * 设置是否构建 AgentScope ReActAgent。 / Set whether to build AgentScope ReActAgent instances.
     *
     * @param buildAgents true 则构建真实 Agent / true if real agents should be built
     */
    public void setBuildAgents(boolean buildAgents) {
        this.buildAgents = buildAgents;
    }

    /**
     * 是否在缺少必要集成组件时直接失败。 / Whether to fail when required integrations are missing.
     *
     * @return true 则启动阶段抛出异常 / true to throw exception at startup
     */
    public boolean isFailOnMissingIntegrations() {
        return failOnMissingIntegrations;
    }

    /**
     * 设置在缺少必要集成组件时是否直接失败。 / Set whether to fail when required integrations are missing.
     *
     * @param failOnMissingIntegrations true 则启动阶段抛出异常 / true to throw exception at startup
     */
    public void setFailOnMissingIntegrations(boolean failOnMissingIntegrations) {
        this.failOnMissingIntegrations = failOnMissingIntegrations;
    }

    /**
     * 获取 Higress MCP 端点 URI。 / Get the Higress MCP endpoint URI.
     *
     * @return MCP 端点 URI / MCP endpoint URI
     */
    public URI getHigressMcpEndpoint() {
        return higressMcpEndpoint;
    }

    /**
     * 设置 Higress MCP 端点 URI。 / Set the Higress MCP endpoint URI.
     *
     * @param higressMcpEndpoint MCP 端点 URI / MCP endpoint URI
     */
    public void setHigressMcpEndpoint(URI higressMcpEndpoint) {
        this.higressMcpEndpoint = higressMcpEndpoint;
    }

    /**
     * 获取 Higress MCP Bearer Token。 / Get the Higress MCP bearer token.
     *
     * @return Bearer Token / bearer token string
     */
    public String getHigressMcpBearerToken() {
        return higressMcpBearerToken;
    }

    /**
     * 设置 Higress MCP Bearer Token。 / Set the Higress MCP bearer token.
     *
     * @param higressMcpBearerToken Bearer Token / bearer token string
     */
    public void setHigressMcpBearerToken(String higressMcpBearerToken) {
        this.higressMcpBearerToken = higressMcpBearerToken;
    }

    /**
     * 获取知识库 HTTP 端点 URI。 / Get the knowledge base HTTP endpoint URI.
     *
     * @return 知识库端点 URI / knowledge base endpoint URI
     */
    public URI getKnowledgeEndpoint() {
        return knowledgeEndpoint;
    }

    /**
     * 设置知识库 HTTP 端点 URI。 / Set the knowledge base HTTP endpoint URI.
     *
     * @param knowledgeEndpoint 知识库端点 URI / knowledge base endpoint URI
     */
    public void setKnowledgeEndpoint(URI knowledgeEndpoint) {
        this.knowledgeEndpoint = knowledgeEndpoint;
    }

    /**
     * 获取知识库 Bearer Token。 / Get the knowledge base bearer token.
     *
     * @return Bearer Token / bearer token string
     */
    public String getKnowledgeBearerToken() {
        return knowledgeBearerToken;
    }

    /**
     * 设置知识库 Bearer Token。 / Set the knowledge base bearer token.
     *
     * @param knowledgeBearerToken Bearer Token / bearer token string
     */
    public void setKnowledgeBearerToken(String knowledgeBearerToken) {
        this.knowledgeBearerToken = knowledgeBearerToken;
    }

    /**
     * 获取外部请求超时时长。 / Get the external request timeout duration.
     *
     * @return 超时 Duration / timeout duration
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * 设置外部请求超时时长。 / Set the external request timeout duration.
     *
     * @param requestTimeout 超时 Duration / timeout duration
     */
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * 获取附加 HTTP 请求头。 / Get the extra HTTP headers.
     *
     * @return 请求头映射 / header key-value map
     */
    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * 设置附加 HTTP 请求头。 / Set the extra HTTP headers.
     *
     * @param extraHeaders 请求头映射 / header key-value map
     */
    public void setExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    /**
     * 获取执行策略配置。 / Get the execution strategy configuration.
     *
     * @return 执行策略对象 / execution config
     */
    public Execution getExecution() {
        return execution;
    }

    /**
     * 设置执行策略配置。 / Set the execution strategy configuration.
     *
     * @param execution 执行策略对象 / execution config
     */
    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    /**
     * 获取记忆配置。
     *
     * @return 记忆配置对象
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * 设置记忆配置。
     *
     * @param memory 记忆配置对象
     */
    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    /**
     * 获取 Nacos 动态配置。 / Get the Nacos dynamic configuration.
     *
     * @return Nacos 配置对象 / Nacos config
     */
    public Nacos getNacos() {
        return nacos;
    }

    /**
     * 设置 Nacos 动态配置。 / Set the Nacos dynamic configuration.
     *
     * @param nacos Nacos 配置对象 / Nacos config
     */
    public void setNacos(Nacos nacos) {
        this.nacos = nacos;
    }

    /**
     * 获取安全边界配置（请求限制、用户检查等）。 / Get the boundary configuration (request limits, user checks, etc.).
     *
     * @return 安全边界对象 / boundary config
     */
    public Boundary getBoundary() {
        return boundary;
    }

    /**
     * 设置安全边界配置。 / Set the boundary configuration.
     *
     * @param boundary 安全边界对象 / boundary config
     */
    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }

    /**
     * 获取部署级运行参数。
     *
     * @return 部署配置对象
     */
    public Deployment getDeployment() {
        return deployment;
    }

    /**
     * 设置部署级运行参数。
     *
     * @param deployment 部署配置对象
     */
    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    /**
     * 获取外部持久化配置。
     *
     * @return 持久化配置
     */
    public Persistence getPersistence() {
        return persistence;
    }

    /**
     * 设置外部持久化配置。
     *
     * @param persistence 持久化配置
     */
    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    /**
     * 获取动态 workflow 存储配置。
     *
     * @return workflow 存储配置
     */
    public WorkflowStore getWorkflowStore() {
        return workflowStore;
    }

    /**
     * 设置动态 workflow 存储配置。
     *
     * @param workflowStore workflow 存储配置
     */
    public void setWorkflowStore(WorkflowStore workflowStore) {
        this.workflowStore = workflowStore;
    }

    /**
     * 外部状态持久化配置。
     */
    public static class Persistence {
        private String type = "memory";
        private String keyPrefix = "dify-to-agentscope";
        private Duration sessionTtl = Duration.ofDays(7);
        private Duration a2aTaskTtl = Duration.ofDays(1);
        private Duration agentStateTtl = Duration.ofDays(7);

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getSessionTtl() {
            return sessionTtl;
        }

        public void setSessionTtl(Duration sessionTtl) {
            this.sessionTtl = sessionTtl;
        }

        public Duration getA2aTaskTtl() {
            return a2aTaskTtl;
        }

        public void setA2aTaskTtl(Duration a2aTaskTtl) {
            this.a2aTaskTtl = a2aTaskTtl;
        }

        public Duration getAgentStateTtl() {
            return agentStateTtl;
        }

        public void setAgentStateTtl(Duration agentStateTtl) {
            this.agentStateTtl = agentStateTtl;
        }
    }

    /**
     * 动态 workflow 定义存储配置。
     */
    public static class WorkflowStore {
        private String type = "memory";
        private String nacosDataIdPrefix = "dify-agentscope-workflow-";
        private String nacosIndexDataId = "dify-agentscope-workflows.json";
        private String nacosGroup = "DEFAULT_GROUP";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getNacosDataIdPrefix() {
            return nacosDataIdPrefix;
        }

        public void setNacosDataIdPrefix(String nacosDataIdPrefix) {
            this.nacosDataIdPrefix = nacosDataIdPrefix;
        }

        public String getNacosIndexDataId() {
            return nacosIndexDataId;
        }

        public void setNacosIndexDataId(String nacosIndexDataId) {
            this.nacosIndexDataId = nacosIndexDataId;
        }

        public String getNacosGroup() {
            return nacosGroup;
        }

        public void setNacosGroup(String nacosGroup) {
            this.nacosGroup = nacosGroup;
        }
    }

    /**
     * 部署级运行参数。
     * <p>这些配置不描述单个 workflow 的业务逻辑，而是约束服务在容器、Kubernetes、多实例等生产环境中的资源使用和状态边界。</p>
     */
    public static class Deployment {
        private boolean stateless;
        private boolean writeGeneratedArtifacts = true;
        private int nodeExecutorThreads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        private int nodeExecutorQueueCapacity = 1000;
        private boolean allowInMemoryState = true;

        /**
         * 判断是否按无状态服务部署。
         *
         * @return true 表示要求当前实例不依赖本地磁盘或本地进程状态
         */
        public boolean isStateless() {
            return stateless;
        }

        /**
         * 设置是否按无状态服务部署。
         *
         * @param stateless true 表示开启无状态部署校验
         */
        public void setStateless(boolean stateless) {
            this.stateless = stateless;
        }

        /**
         * 判断是否写出转换后的 workflow 审计产物。
         *
         * @return true 表示启动或动态创建 workflow 时写入 generated-output-dir
         */
        public boolean isWriteGeneratedArtifacts() {
            return writeGeneratedArtifacts;
        }

        /**
         * 设置是否写出转换后的 workflow 审计产物。
         *
         * @param writeGeneratedArtifacts true 表示允许写本地审计产物
         */
        public void setWriteGeneratedArtifacts(boolean writeGeneratedArtifacts) {
            this.writeGeneratedArtifacts = writeGeneratedArtifacts;
        }

        /**
         * 获取节点执行线程数。
         *
         * @return 共享节点线程池固定线程数
         */
        public int getNodeExecutorThreads() {
            return nodeExecutorThreads;
        }

        /**
         * 设置节点执行线程数。
         *
         * @param nodeExecutorThreads 共享节点线程池固定线程数
         */
        public void setNodeExecutorThreads(int nodeExecutorThreads) {
            this.nodeExecutorThreads = nodeExecutorThreads;
        }

        /**
         * 获取节点执行队列容量。
         *
         * @return 队列容量，超过后请求会快速失败
         */
        public int getNodeExecutorQueueCapacity() {
            return nodeExecutorQueueCapacity;
        }

        /**
         * 设置节点执行队列容量。
         *
         * @param nodeExecutorQueueCapacity 队列容量
         */
        public void setNodeExecutorQueueCapacity(int nodeExecutorQueueCapacity) {
            this.nodeExecutorQueueCapacity = nodeExecutorQueueCapacity;
        }

        /**
         * 判断是否允许使用进程内状态实现。
         *
         * @return true 表示允许内存 session、内存 AgentStateStore 等本地状态
         */
        public boolean isAllowInMemoryState() {
            return allowInMemoryState;
        }

        /**
         * 设置是否允许使用进程内状态实现。
         *
         * @param allowInMemoryState true 表示允许本地内存状态，false 表示生产启动时做强校验
         */
        public void setAllowInMemoryState(boolean allowInMemoryState) {
            this.allowInMemoryState = allowInMemoryState;
        }
    }

    /**
     * 运行时安全边界配置。
     * <p>
     * 控制请求长度上限、输出长度上限、是否强制要求用户 ID，
     * 以及是否允许在缺少真实集成时使用桩（Stub）实现。
     */
    public static class Boundary {
        private int maxQueryLength = 4000;
        private int maxOutputLength = 8000;
        private boolean requireUserId = true;
        private boolean allowStubIntegrations = true;

        /**
         * 获取最大查询长度。 / Get the maximum query length.
         *
         * @return 最大查询字符数 / max query character count
         */
        public int getMaxQueryLength() {
            return maxQueryLength;
        }

        /**
         * 设置最大查询长度。 / Set the maximum query length.
         *
         * @param maxQueryLength 最大查询字符数 / max query character count
         */
        public void setMaxQueryLength(int maxQueryLength) {
            this.maxQueryLength = maxQueryLength;
        }

        /**
         * 获取最大输出长度。 / Get the maximum output length.
         *
         * @return 最大输出字符数 / max output character count
         */
        public int getMaxOutputLength() {
            return maxOutputLength;
        }

        /**
         * 设置最大输出长度。 / Set the maximum output length.
         *
         * @param maxOutputLength 最大输出字符数 / max output character count
         */
        public void setMaxOutputLength(int maxOutputLength) {
            this.maxOutputLength = maxOutputLength;
        }

        /**
         * 是否要求请求携带用户 ID。 / Whether user ID is required in requests.
         *
         * @return true 则必须提供用户 ID / true if user id is mandatory
         */
        public boolean isRequireUserId() {
            return requireUserId;
        }

        /**
         * 设置是否要求请求携带用户 ID。 / Set whether user ID is required in requests.
         *
         * @param requireUserId true 则必须提供用户 ID / true if user id is mandatory
         */
        public void setRequireUserId(boolean requireUserId) {
            this.requireUserId = requireUserId;
        }

        /**
         * 是否允许使用桩实现（Stub）替代真实集成。 / Whether stub implementations are allowed.
         *
         * @return true 则允许 Stub / true to allow stubs
         */
        public boolean isAllowStubIntegrations() {
            return allowStubIntegrations;
        }

        /**
         * 设置是否允许使用桩实现。 / Set whether stub implementations are allowed.
         *
         * @param allowStubIntegrations true 则允许 Stub / true to allow stubs
         */
        public void setAllowStubIntegrations(boolean allowStubIntegrations) {
            this.allowStubIntegrations = allowStubIntegrations;
        }
    }

    /**
     * 工作流执行策略配置。
     * <p>
     * 包括最大执行步骤数、Agent 内部兜底应答内容，以及 demo 标识符的提取规则。
     */
    public static class Execution {
        private int maxSteps = 100;
        private Duration defaultNodeTimeout = Duration.ofSeconds(30);
        private Map<String, Duration> nodeTimeouts = new LinkedHashMap<>();
        private String fallbackAnswer = "当前系统繁忙，无法联网工作，请稍后重试！";
        private IdentifierRules identifierRules = new IdentifierRules();

        /**
         * 获取最大执行步骤数。 / Get the maximum number of execution steps.
         *
         * @return 最大步骤数 / max steps
         */
        public int getMaxSteps() {
            return maxSteps;
        }

        /**
         * 设置最大执行步骤数。 / Set the maximum number of execution steps.
         *
         * @param maxSteps 最大步骤数 / max steps
         */
        public void setMaxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
        }

        /**
         * 获取单个节点默认最大执行时长。
         *
         * @return 默认节点超时时长
         */
        public Duration getDefaultNodeTimeout() {
            return defaultNodeTimeout;
        }

        /**
         * 设置单个节点默认最大执行时长。
         *
         * @param defaultNodeTimeout 默认节点超时时长，0 或负数表示关闭节点级超时
         */
        public void setDefaultNodeTimeout(Duration defaultNodeTimeout) {
            this.defaultNodeTimeout = defaultNodeTimeout;
        }

        /**
         * 获取节点 ID 或节点类型维度的超时覆盖配置。
         *
         * @return 节点 ID/类型 → 超时时长
         */
        public Map<String, Duration> getNodeTimeouts() {
            return nodeTimeouts;
        }

        /**
         * 设置节点 ID 或节点类型维度的超时覆盖配置。
         *
         * @param nodeTimeouts 节点 ID/类型 → 超时时长
         */
        public void setNodeTimeouts(Map<String, Duration> nodeTimeouts) {
            this.nodeTimeouts = nodeTimeouts;
        }

        /**
         * 获取兜底应答内容（Agent 无法正常回复时使用）。 / Get the fallback answer used when agent cannot respond.
         *
         * @return 兜底应答文本 / fallback answer text
         */
        public String getFallbackAnswer() {
            return fallbackAnswer;
        }

        /**
         * 设置兜底应答内容。 / Set the fallback answer text.
         *
         * @param fallbackAnswer 兜底应答文本 / fallback answer text
         */
        public void setFallbackAnswer(String fallbackAnswer) {
            this.fallbackAnswer = fallbackAnswer;
        }

        /**
         * 获取标识符提取规则。 / Get the identifier extraction rules.
         *
         * @return 标识符规则对象 / identifier rules
         */
        public IdentifierRules getIdentifierRules() {
            return identifierRules;
        }

        /**
         * 设置标识符提取规则。 / Set the identifier extraction rules.
         *
         * @param identifierRules 标识符规则对象 / identifier rules
         */
        public void setIdentifierRules(IdentifierRules identifierRules) {
            this.identifierRules = identifierRules;
        }
    }

    /**
     * 记忆能力配置。
     * <p>包含短期 session memory 和 Memos 长期记忆两类配置。</p>
     */
    public static class Memory {
        private Session session = new Session();
        private Memos memos = new Memos();

        /**
         * 获取短期会话记忆配置。
         *
         * @return session memory 配置
         */
        public Session getSession() {
            return session;
        }

        /**
         * 设置短期会话记忆配置。
         *
         * @param session session memory 配置
         */
        public void setSession(Session session) {
            this.session = session;
        }

        /**
         * 获取 Memos 长期记忆配置。
         *
         * @return Memos 配置
         */
        public Memos getMemos() {
            return memos;
        }

        /**
         * 设置 Memos 长期记忆配置。
         *
         * @param memos Memos 配置
         */
        public void setMemos(Memos memos) {
            this.memos = memos;
        }
    }

    /**
     * 短期会话记忆配置。
     * <p>用于控制是否在服务端保存最近多轮 conversation。</p>
     */
    public static class Session {
        private boolean enabled = true;
        private int maxTurns = 20;

        /**
         * 判断是否启用短期会话记忆。
         *
         * @return true 表示启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用短期会话记忆。
         *
         * @param enabled true 表示启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取每个会话最多保留的轮次数。
         *
         * @return 最大轮次数
         */
        public int getMaxTurns() {
            return maxTurns;
        }

        /**
         * 设置每个会话最多保留的轮次数。
         *
         * @param maxTurns 最大轮次数
         */
        public void setMaxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
        }
    }

    /**
     * Memos 长期记忆配置。
     * <p>启用后，memo-retrieval 节点和 memory HTTP 接口会访问 Memos API。</p>
     */
    public static class Memos {
        private boolean enabled;
        private URI endpoint;
        private String accessToken;
        private String visibility = "PRIVATE";
        private int defaultLimit = 5;

        /**
         * 判断是否启用 Memos 集成。
         *
         * @return true 表示启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 Memos 集成。
         *
         * @param enabled true 表示启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 Memos 服务地址。
         *
         * @return Memos endpoint
         */
        public URI getEndpoint() {
            return endpoint;
        }

        /**
         * 设置 Memos 服务地址。
         *
         * @param endpoint Memos endpoint
         */
        public void setEndpoint(URI endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * 获取 Memos 访问令牌。
         *
         * @return access token
         */
        public String getAccessToken() {
            return accessToken;
        }

        /**
         * 设置 Memos 访问令牌。
         *
         * @param accessToken access token
         */
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        /**
         * 获取新写入 memo 的默认可见性。
         *
         * @return Memos visibility
         */
        public String getVisibility() {
            return visibility;
        }

        /**
         * 设置新写入 memo 的默认可见性。
         *
         * @param visibility Memos visibility
         */
        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        /**
         * 获取默认检索条数。
         *
         * @return 默认 limit
         */
        public int getDefaultLimit() {
            return defaultLimit;
        }

        /**
         * 设置默认检索条数。
         *
         * @param defaultLimit 默认 limit
         */
        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }
    }

    /**
     * demo 标识符的正则匹配规则。
     * <p>字段名沿用历史配置键以保持兼容，默认值不包含具体业务语义。</p>
     */
    public static class IdentifierRules {
        private String demoItem = "ITEM-\\d{4}";
        private String demoRequest = "REQ-\\d{4}";
        private String demoTask = "TASK-\\d{4}";

        /**
         * 获取 demo item 正则。 / Get the demo item regex pattern.
         *
         * @return demo item 正则
         */
        public String getDemoItem() {
            return demoItem;
        }

        /**
         * 设置 demo item 正则。 / Set the demo item regex pattern.
         *
         * @param demoItem demo item 正则
         */
        public void setDemoItem(String demoItem) {
            this.demoItem = demoItem;
        }

        /**
         * 获取 demo request 正则。 / Get the demo request regex pattern.
         *
         * @return demo request 正则
         */
        public String getDemoRequest() {
            return demoRequest;
        }

        /**
         * 设置 demo request 正则。 / Set the demo request regex pattern.
         *
         * @param demoRequest demo request 正则
         */
        public void setDemoRequest(String demoRequest) {
            this.demoRequest = demoRequest;
        }

        /**
         * 获取 demo task 正则。 / Get the demo task regex pattern.
         *
         * @return demo task 正则
         */
        public String getDemoTask() {
            return demoTask;
        }

        /**
         * 设置 demo task 正则。 / Set the demo task regex pattern.
         *
         * @param demoTask demo task 正则
         */
        public void setDemoTask(String demoTask) {
            this.demoTask = demoTask;
        }

        /**
         * 兼容旧配置键 waybill，内部映射到 demo item。
         *
         * @return demo item 正则
         */
        public String getWaybill() {
            return demoItem;
        }

        /**
         * 兼容旧配置键 waybill，内部映射到 demo item。
         *
         * @param waybill demo item 正则
         */
        public void setWaybill(String waybill) {
            this.demoItem = waybill;
        }

        /**
         * 兼容旧配置键 order，内部映射到 demo request。
         *
         * @return demo request 正则
         */
        public String getOrder() {
            return demoRequest;
        }

        /**
         * 兼容旧配置键 order，内部映射到 demo request。
         *
         * @param order demo request 正则
         */
        public void setOrder(String order) {
            this.demoRequest = order;
        }

        /**
         * 兼容旧配置键 work，内部映射到 demo task。
         *
         * @return demo task 正则
         */
        public String getWork() {
            return demoTask;
        }

        /**
         * 兼容旧配置键 work，内部映射到 demo task。
         *
         * @param work demo task 正则
         */
        public void setWork(String work) {
            this.demoTask = work;
        }
    }

    /**
     * Nacos 配置中心连接与资源同步配置。
     * <p>
     * 控制 Nacos 启停开关、服务地址、命名空间、认证信息，
     * 以及 Prompt / Skill / MCP 等资源的同步粒度（dataId、版本、标签等）。
     */
    public static class Nacos {
        private boolean enabled;
        private String serverAddr;
        private String namespace = "public";
        private String username;
        private String password;
        private String accessKey;
        private String secretKey;
        private Prompt prompt = new Prompt();
        private Skill skill = new Skill();
        private Mcp mcp = new Mcp();

        /**
         * Nacos 是否启用。 / Whether Nacos is enabled.
         *
         * @return true 则启用 Nacos / true if Nacos is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 Nacos 是否启用。 / Set whether Nacos is enabled.
         *
         * @param enabled true 则启用 Nacos / true if Nacos is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 Nacos 服务器地址。 / Get the Nacos server address.
         *
         * @return 服务器地址 / server address
         */
        public String getServerAddr() {
            return serverAddr;
        }

        /**
         * 设置 Nacos 服务器地址。 / Set the Nacos server address.
         *
         * @param serverAddr 服务器地址 / server address
         */
        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        /**
         * 获取 Nacos 命名空间。 / Get the Nacos namespace.
         *
         * @return 命名空间 / namespace
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * 设置 Nacos 命名空间。 / Set the Nacos namespace.
         *
         * @param namespace 命名空间 / namespace
         */
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        /**
         * 获取 Nacos 用户名。 / Get the Nacos username.
         *
         * @return 用户名 / username
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置 Nacos 用户名。 / Set the Nacos username.
         *
         * @param username 用户名 / username
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 获取 Nacos 密码。 / Get the Nacos password.
         *
         * @return 密码 / password
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置 Nacos 密码。 / Set the Nacos password.
         *
         * @param password 密码 / password
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 获取 RAM AccessKey（阿里云 Nacos 鉴权）。 / Get the RAM AccessKey (Alibaba Cloud Nacos auth).
         *
         * @return AccessKey / access key
         */
        public String getAccessKey() {
            return accessKey;
        }

        /**
         * 设置 RAM AccessKey。 / Set the RAM AccessKey.
         *
         * @param accessKey AccessKey / access key
         */
        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        /**
         * 获取 RAM SecretKey。 / Get the RAM SecretKey.
         *
         * @return SecretKey / secret key
         */
        public String getSecretKey() {
            return secretKey;
        }

        /**
         * 设置 RAM SecretKey。 / Set the RAM SecretKey.
         *
         * @param secretKey SecretKey / secret key
         */
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        /**
         * 获取 Prompt 同步配置。 / Get the Prompt synchronization configuration.
         *
         * @return Prompt 配置 / prompt config
         */
        public Prompt getPrompt() {
            return prompt;
        }

        /**
         * 设置 Prompt 同步配置。 / Set the Prompt synchronization configuration.
         *
         * @param prompt Prompt 配置 / prompt config
         */
        public void setPrompt(Prompt prompt) {
            this.prompt = prompt;
        }

        /**
         * 获取 Skill 同步配置。 / Get the Skill synchronization configuration.
         *
         * @return Skill 配置 / skill config
         */
        public Skill getSkill() {
            return skill;
        }

        /**
         * 设置 Skill 同步配置。 / Set the Skill synchronization configuration.
         *
         * @param skill Skill 配置 / skill config
         */
        public void setSkill(Skill skill) {
            this.skill = skill;
        }

        /**
         * 获取 MCP 服务同步配置。 / Get the MCP server synchronization configuration.
         *
         * @return MCP 配置 / mcp config
         */
        public Mcp getMcp() {
            return mcp;
        }

        /**
         * 设置 MCP 服务同步配置。 / Set the MCP server synchronization configuration.
         *
         * @param mcp MCP 配置 / mcp config
         */
        public void setMcp(Mcp mcp) {
            this.mcp = mcp;
        }
    }

    /**
     * Nacos Prompt 资源同步配置。
     * <p>
     * 定义需要从 Nacos 同步的 Prompt 列表、目标版本号及分组标签。
     */
    public static class Prompt {
        private boolean enabled = true;
        private List<String> keys = new ArrayList<>();
        private String version;
        private String label;

        /**
         * Prompt 同步是否启用。 / Whether Prompt synchronization is enabled.
         *
         * @return true 则启用同步 / true if enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 Prompt 同步是否启用。 / Set whether Prompt synchronization is enabled.
         *
         * @param enabled true 则启用同步 / true if enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取要同步的 Prompt dataId 列表。 / Get the list of Prompt dataIds to sync.
         *
         * @return dataId 列表 / list of dataIds
         */
        public List<String> getKeys() {
            return keys;
        }

        /**
         * 设置要同步的 Prompt dataId 列表。 / Set the list of Prompt dataIds to sync.
         *
         * @param keys dataId 列表 / list of dataIds
         */
        public void setKeys(List<String> keys) {
            this.keys = keys;
        }

        /**
         * 获取 Prompt 目标版本号。 / Get the target version for Prompt.
         *
         * @return 版本号 / version
         */
        public String getVersion() {
            return version;
        }

        /**
         * 设置 Prompt 目标版本号。 / Set the target version for Prompt.
         *
         * @param version 版本号 / version
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 获取 Prompt 分组标签。 / Get the label for Prompt grouping.
         *
         * @return 标签 / label
         */
        public String getLabel() {
            return label;
        }

        /**
         * 设置 Prompt 分组标签。 / Set the label for Prompt grouping.
         *
         * @param label 标签 / label
         */
        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * Nacos Skill 资源同步配置。
     * <p>
     * 定义需要从 Nacos 同步的 Skill 名称列表、目标版本号及分组标签。
     */
    public static class Skill {
        private boolean enabled = true;
        private List<String> names = new ArrayList<>();
        private String version;
        private String label;

        /**
         * Skill 同步是否启用。 / Whether Skill synchronization is enabled.
         *
         * @return true 则启用同步 / true if enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 Skill 同步是否启用。 / Set whether Skill synchronization is enabled.
         *
         * @param enabled true 则启用同步 / true if enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取要同步的 Skill 名称列表。 / Get the list of Skill names to sync.
         *
         * @return Skill 名称列表 / list of skill names
         */
        public List<String> getNames() {
            return names;
        }

        /**
         * 设置要同步的 Skill 名称列表。 / Set the list of Skill names to sync.
         *
         * @param names Skill 名称列表 / list of skill names
         */
        public void setNames(List<String> names) {
            this.names = names;
        }

        /**
         * 获取 Skill 目标版本号。 / Get the target version for Skill.
         *
         * @return 版本号 / version
         */
        public String getVersion() {
            return version;
        }

        /**
         * 设置 Skill 目标版本号。 / Set the target version for Skill.
         *
         * @param version 版本号 / version
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 获取 Skill 分组标签。 / Get the label for Skill grouping.
         *
         * @return 标签 / label
         */
        public String getLabel() {
            return label;
        }

        /**
         * 设置 Skill 分组标签。 / Set the label for Skill grouping.
         *
         * @param label 标签 / label
         */
        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * Nacos MCP 服务同步配置。
     * <p>
     * 控制是否从 Nacos 同步 MCP（Model Context Protocol）服务定义，
     * 并维护一组 {@link McpServer} 端点配置。
     */
    public static class Mcp {
        private boolean enabled = true;
        private Map<String, McpServer> servers = new LinkedHashMap<>();

        /**
         * MCP 同步是否启用。 / Whether MCP synchronization is enabled.
         *
         * @return true 则启用同步 / true if enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 MCP 同步是否启用。 / Set whether MCP synchronization is enabled.
         *
         * @param enabled true 则启用同步 / true if enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 MCP 服务端映射表。 / Get the MCP server map.
         *
         * @return 服务名 → 服务端配置 / server name → server config
         */
        public Map<String, McpServer> getServers() {
            return servers;
        }

        /**
         * 设置 MCP 服务端映射表。 / Set the MCP server map.
         *
         * @param servers 服务名 → 服务端配置 / server name → server config
         */
        public void setServers(Map<String, McpServer> servers) {
            this.servers = servers;
        }
    }

    /**
     * 单个 MCP 服务端的 Nacos 资源坐标。
     * <p>
     * 包含端点地址、Nacos dataId、分组、版本号及标签，
     * 用于从 Nacos 配置中心定位并加载 MCP 服务定义。
     */
    public static class McpServer {
        private String endpoint;
        private String dataId;
        private String group = "DEFAULT_GROUP";
        private String version;
        private String label;

        /**
         * 获取 MCP 服务端点地址。 / Get the MCP server endpoint address.
         *
         * @return 端点地址 / endpoint
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * 设置 MCP 服务端点地址。 / Set the MCP server endpoint address.
         *
         * @param endpoint 端点地址 / endpoint
         */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * 获取 Nacos dataId。 / Get the Nacos dataId.
         *
         * @return dataId
         */
        public String getDataId() {
            return dataId;
        }

        /**
         * 设置 Nacos dataId。 / Set the Nacos dataId.
         *
         * @param dataId dataId
         */
        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        /**
         * 获取 Nacos 分组。 / Get the Nacos group.
         *
         * @return 分组 / group
         */
        public String getGroup() {
            return group;
        }

        /**
         * 设置 Nacos 分组。 / Set the Nacos group.
         *
         * @param group 分组 / group
         */
        public void setGroup(String group) {
            this.group = group;
        }

        /**
         * 获取资源版本号。 / Get the resource version.
         *
         * @return 版本号 / version
         */
        public String getVersion() {
            return version;
        }

        /**
         * 设置资源版本号。 / Set the resource version.
         *
         * @param version 版本号 / version
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 获取分组标签。 / Get the grouping label.
         *
         * @return 标签 / label
         */
        public String getLabel() {
            return label;
        }

        /**
         * 设置分组标签。 / Set the grouping label.
         *
         * @param label 标签 / label
         */
        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * 单个工作流的源定义。
     * <p>
     * {@code sourceType=dify} 表示从 Dify DSL 自动转换；{@code sourceType=agentscope}
     * 表示读取手写的 AgentScope-native workflow YAML。
     */
    public static class WorkflowDefinition {
        private String sourceType = "dify";
        private String location;
        private String dslLocation = "classpath:workflows/default.dsl";

        /**
         * 获取工作流来源类型。
         *
         * @return dify 或 agentscope 等来源类型
         */
        public String getSourceType() {
            return sourceType;
        }

        /**
         * 设置工作流来源类型。
         *
         * @param sourceType dify 或 agentscope 等来源类型
         */
        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        /**
         * 获取工作流资源位置。
         * <p>未显式配置 location 时回退到兼容字段 dslLocation。</p>
         *
         * @return workflow 资源位置
         */
        public String getLocation() {
            return location == null || location.isBlank() ? dslLocation : location;
        }

        /**
         * 设置工作流资源位置。
         *
         * @param location workflow 资源位置
         */
        public void setLocation(String location) {
            this.location = location;
        }

        /**
         * 获取 DSL 文件位置。 / Get the DSL file location.
         *
         * @return DSL 路径 / DSL resource location
         */
        public String getDslLocation() {
            return dslLocation;
        }

        /**
         * 设置 DSL 文件位置。 / Set the DSL file location.
         *
         * @param dslLocation DSL 路径 / DSL resource location
         */
        public void setDslLocation(String dslLocation) {
            this.dslLocation = dslLocation;
            if (this.location == null || this.location.isBlank()) {
                this.location = dslLocation;
            }
        }
    }

    /**
     * 构造默认工作流映射。
     * <p>
     * 预置一个名为 {@code "default"} 的 {@link WorkflowDefinition} 条目，
     * 指向 classpath 下的默认 DSL 文件。
     *
     * @return 默认工作流映射 / default workflow map
     */
    private Map<String, WorkflowDefinition> defaultWorkflows() {
        Map<String, WorkflowDefinition> defaults = new LinkedHashMap<>();
        defaults.put("default", new WorkflowDefinition());
        return defaults;
    }
}
