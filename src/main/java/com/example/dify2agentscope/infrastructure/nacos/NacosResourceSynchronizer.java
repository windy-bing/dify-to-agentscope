package com.example.dify2agentscope.infrastructure.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.model.prompt.Prompt;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.example.dify2agentscope.config.DifyRuntimeProperties;
import com.example.dify2agentscope.domain.nacos.NacosResourceCatalog;
import com.example.dify2agentscope.domain.nacos.NacosResourceItem;
import com.example.dify2agentscope.domain.nacos.NacosResourceType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从 Nacos 同步资源（prompt / skill / MCP Server 配置），构建 NacosResourceCatalog。
 * <p>
 * Synchronize resources (prompt / skill / MCP Server config) from Nacos and build NacosResourceCatalog.
 */
public class NacosResourceSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(NacosResourceSynchronizer.class);
    private static final long CONFIG_TIMEOUT_MS = 5_000L;

    private final DifyRuntimeProperties properties;

    /**
     * 创建 Nacos 资源同步器。
     * <p>
     * Create Nacos resource synchronizer.
     *
     * @param properties Dify 运行时配置 / Dify runtime properties
     */
    public NacosResourceSynchronizer(DifyRuntimeProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行一次完整的资源加载，从 Nacos 同步所有已启用的资源类型。
     * <p>
     * Perform a full resource load, syncing all enabled resource types from Nacos.
     *
     * @return 包含所有已加载资源的目录 / catalog containing all loaded resources
     */
    public NacosResourceCatalog load() {
        DifyRuntimeProperties.Nacos nacos = properties.getNacos();
        if (!nacos.isEnabled()) {
            log.info("Nacos \u672a\u542f\u7528\uff0c\u8df3\u8fc7 prompt\u3001skill\u3001mcp \u8d44\u6e90\u540c\u6b65");
            return InMemoryNacosResourceCatalog.empty();
        }
        try {
            Properties nacosProperties = nacosProperties(nacos);
            AiService aiService = AiFactory.createAiService(nacosProperties);
            ConfigService configService = NacosFactory.createConfigService(nacosProperties);
            Map<String, NacosResourceItem> prompts = loadPrompts(aiService, nacos);
            Map<String, NacosResourceItem> skills = loadSkills(aiService, nacos);
            Map<String, NacosResourceItem> mcpServers = loadMcpServers(configService, nacos);
            log.info(
                    "Nacos \u8d44\u6e90\u540c\u6b65\u5b8c\u6210\uff0cprompts={}\uff0cskills={}\uff0cmcpServers={}",
                    prompts.size(),
                    skills.size(),
                    mcpServers.size());
            return new InMemoryNacosResourceCatalog(prompts, skills, mcpServers);
        } catch (Exception e) {
            if (properties.isFailOnMissingIntegrations()) {
                throw new IllegalStateException("Nacos \u8d44\u6e90\u540c\u6b65\u5931\u8d25", e);
            }
            log.warn("Nacos \u8d44\u6e90\u540c\u6b65\u5931\u8d25\uff0c\u4f7f\u7528\u7a7a\u8d44\u6e90\u76ee\u5f55: {}", e.getMessage());
            return InMemoryNacosResourceCatalog.empty();
        }
    }

    /**
     * 加载 Nacos Prompt 资源列表。
     * <p>
     * Load Nacos prompt resources.
     *
     * @param aiService Nacos AI 服务 / Nacos AI service
     * @param nacos     Nacos 配置 / Nacos configuration
     * @return prompt 名称到资源项的映射 / mapping from prompt name to resource item
     */
    private Map<String, NacosResourceItem> loadPrompts(
            AiService aiService, DifyRuntimeProperties.Nacos nacos) {
        Map<String, NacosResourceItem> prompts = new LinkedHashMap<>();
        if (!nacos.getPrompt().isEnabled()) {
            return prompts;
        }
        for (String key : nacos.getPrompt().getKeys()) {
            if (isBlank(key)) {
                continue;
            }
            try {
                Prompt prompt = aiService.subscribePrompt(
                        key,
                        blankToNull(nacos.getPrompt().getVersion()),
                        blankToNull(nacos.getPrompt().getLabel()),
                        null);
                if (prompt == null || prompt.getTemplate() == null) {
                    log.warn("Nacos prompt \u672a\u627e\u5230: {}", key);
                    continue;
                }
                prompts.put(key, new NacosResourceItem(
                        key,
                        NacosResourceType.PROMPT,
                        prompt.getTemplate(),
                        null,
                        Map.of("version", valueOrEmpty(prompt.getVersion())),
                        Instant.now()));
                log.info("\u5df2\u52a0\u8f7d Nacos prompt: {}", key);
            } catch (NacosException e) {
                handleItemFailure("prompt", key, e);
            }
        }
        return prompts;
    }

    /**
     * 加载 Nacos Skill 资源列表。
     * <p>
     * Load Nacos skill resources.
     *
     * @param aiService Nacos AI 服务 / Nacos AI service
     * @param nacos     Nacos 配置 / Nacos configuration
     * @return skill 名称到资源项的映射 / mapping from skill name to resource item
     */
    private Map<String, NacosResourceItem> loadSkills(
            AiService aiService, DifyRuntimeProperties.Nacos nacos) {
        Map<String, NacosResourceItem> skills = new LinkedHashMap<>();
        if (!nacos.getSkill().isEnabled()) {
            return skills;
        }
        for (String name : nacos.getSkill().getNames()) {
            if (isBlank(name)) {
                continue;
            }
            try {
                byte[] zip = downloadSkill(aiService, name, nacos.getSkill());
                if (zip == null || zip.length == 0) {
                    log.warn("Nacos skill \u672a\u627e\u5230: {}", name);
                    continue;
                }
                skills.put(name, new NacosResourceItem(
                        name,
                        NacosResourceType.SKILL,
                        "",
                        zip,
                        Map.of("bytes", zip.length),
                        Instant.now()));
                log.info("\u5df2\u52a0\u8f7d Nacos skill: {}\uff0c\u5927\u5c0f={} bytes", name, zip.length);
            } catch (NacosException e) {
                handleItemFailure("skill", name, e);
            }
        }
        return skills;
    }

    /**
     * 根据版本或标签从 Nacos 下载 Skill 的 ZIP 包。
     * <p>
     * Download Skill ZIP package from Nacos by version or label.
     *
     * @param aiService Nacos AI 服务 / Nacos AI service
     * @param name      skill 名称 / skill name
     * @param skill     skill 配置项 / skill configuration
     * @return ZIP 二进制数据 / ZIP binary data
     * @throws NacosException Nacos 调用异常 / Nacos invocation exception
     */
    private byte[] downloadSkill(
            AiService aiService,
            String name,
            DifyRuntimeProperties.Skill skill)
            throws NacosException {
        if (!isBlank(skill.getVersion())) {
            return aiService.downloadSkillZipByVersion(name, skill.getVersion());
        }
        if (!isBlank(skill.getLabel())) {
            return aiService.downloadSkillZipByLabel(name, skill.getLabel());
        }
        return aiService.downloadSkillZip(name);
    }

    /**
     * 加载 Nacos MCP Server 配置列表。
     * <p>
     * Load Nacos MCP Server configurations.
     *
     * @param configService Nacos 配置服务 / Nacos config service
     * @param nacos         Nacos 配置 / Nacos configuration
     * @return MCP Server 名称到资源项的映射 / mapping from MCP Server name to resource item
     */
    private Map<String, NacosResourceItem> loadMcpServers(
            ConfigService configService, DifyRuntimeProperties.Nacos nacos) {
        Map<String, NacosResourceItem> mcpServers = new LinkedHashMap<>();
        if (!nacos.getMcp().isEnabled()) {
            return mcpServers;
        }
        nacos.getMcp().getServers().forEach((name, server) -> {
            if (isBlank(name)) {
                return;
            }
            try {
                String content = server.getEndpoint();
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("endpoint", valueOrEmpty(server.getEndpoint()));
                metadata.put("version", valueOrEmpty(server.getVersion()));
                metadata.put("label", valueOrEmpty(server.getLabel()));
                if (!isBlank(server.getDataId())) {
                    content = configService.getConfig(
                            server.getDataId(),
                            isBlank(server.getGroup()) ? "DEFAULT_GROUP" : server.getGroup(),
                            CONFIG_TIMEOUT_MS);
                    metadata.put("dataId", server.getDataId());
                    metadata.put("group", isBlank(server.getGroup()) ? "DEFAULT_GROUP" : server.getGroup());
                }
                if (isBlank(content)) {
                    log.warn("Nacos MCP \u914d\u7f6e\u4e3a\u7a7a: {}", name);
                    return;
                }
                mcpServers.put(name, new NacosResourceItem(
                        name,
                        NacosResourceType.MCP_SERVER,
                        content,
                        null,
                        metadata,
                        Instant.now()));
                log.info("\u5df2\u52a0\u8f7d Nacos MCP \u914d\u7f6e: {}", name);
            } catch (NacosException e) {
                handleItemFailure("mcp", name, e);
            }
        });
        return mcpServers;
    }

    /**
     * 构建 Nacos 连接 Properties。
     * <p>
     * Build Nacos connection Properties.
     *
     * @param nacos Nacos 配置 / Nacos configuration
     * @return Nacos 客户端属性 / Nacos client properties
     */
    private Properties nacosProperties(DifyRuntimeProperties.Nacos nacos) {
        if (isBlank(nacos.getServerAddr())) {
            throw new IllegalArgumentException("Nacos server-addr \u4e0d\u80fd\u4e3a\u7a7a");
        }
        Properties result = new Properties();
        result.put("serverAddr", nacos.getServerAddr());
        result.put("namespace", isBlank(nacos.getNamespace()) ? "public" : nacos.getNamespace());
        putIfNotBlank(result, "username", nacos.getUsername());
        putIfNotBlank(result, "password", nacos.getPassword());
        putIfNotBlank(result, "accessKey", nacos.getAccessKey());
        putIfNotBlank(result, "secretKey", nacos.getSecretKey());
        return result;
    }

    /**
     * 处理单个资源项加载失败的情况，根据配置决定抛出异常或仅记录警告。
     * <p>
     * Handle individual resource item load failure, throwing exception or logging warning based on config.
     *
     * @param type 资源类型 / resource type
     * @param name 资源名称 / resource name
     * @param e    异常 / exception
     */
    private void handleItemFailure(String type, String name, Exception e) {
        if (properties.isFailOnMissingIntegrations()) {
            throw new IllegalStateException("Nacos " + type + " \u52a0\u8f7d\u5931\u8d25: " + name, e);
        }
        log.warn("Nacos {} \u52a0\u8f7d\u5931\u8d25: {}\uff0c\u539f\u56e0={}", type, name, e.getMessage());
    }

    /**
     * 仅当值非空时放入 Properties。
     * <p>
     * Put into Properties only when value is not blank.
     *
     * @param properties 目标 Properties / target properties
     * @param key        键 / key
     * @param value      值 / value
     */
    private void putIfNotBlank(Properties properties, String key, String value) {
        if (!isBlank(value)) {
            properties.put(key, value);
        }
    }

    /**
     * 将空字符串转为 null。
     * <p>
     * Convert blank string to null.
     *
     * @param value 输入字符串 / input string
     * @return null 或原值 / null or original value
     */
    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    /**
     * 将对象转换为字符串，null 时返回空字符串。
     * <p>
     * Convert object to string, returning empty string for null.
     *
     * @param value 输入对象 / input object
     * @return 非 null 字符串 / non-null string
     */
    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 判断字符串是否为 null 或空白。
     * <p>
     * Check if a string is null or blank.
     *
     * @param value 输入字符串 / input string
     * @return 如果为 null 或空白返回 true / true if null or blank
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
