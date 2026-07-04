package com.example.dify2agentscope.domain.agentscope;

/**
 * AgentScope 官方能力在当前项目中的落地状态。
 * <p>该枚举用于把“已经接入官方 API”“项目自研适配层”“仅保留扩展点”明确区分开，避免后续把项目自定义
 * workflow 能力误认为 AgentScope Java 官方原生实现。</p>
 */
public enum AgentScopeCapabilityStatus {

    /** 已直接使用 AgentScope Java 官方 API 或官方约定。 */
    OFFICIAL_API_USED,

    /** 官方有对应概念，但当前项目只保留了最小适配层或简化实现。 */
    PROJECT_MINIMAL_ADAPTER,

    /** 官方有完整能力，当前项目仅保留扩展锚点，暂未接入真实执行。 */
    EXTENSION_POINT_ONLY,

    /** 当前能力是本项目为了 Dify/workflow 迁移自定义的，不属于 AgentScope 官方核心能力。 */
    PROJECT_DEFINED
}
