package com.example.dify2agentscope;

import com.example.dify2agentscope.config.DifyRuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Dify → AgentScope 转换引擎 Spring Boot 应用入口。
 * <p>
 * 启用 {@link DifyRuntimeProperties} 配置绑定，启动后自动扫描并装配
 * 工作流解析、AgentScope Agent 构建、Nacos 动态配置同步以及运行时
 * 安全策略等全部基础设施 Bean。
 */
@SpringBootApplication
@EnableConfigurationProperties(DifyRuntimeProperties.class)
public class DifyToAgentScopeApplication {

    /**
     * 应用主入口方法。
     * <p>
     * 委托给 {@link SpringApplication#run(Class, String[])} 启动
     * Spring 应用上下文。
     *
     * @param args 命令行参数 / command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DifyToAgentScopeApplication.class, args);
    }
}
