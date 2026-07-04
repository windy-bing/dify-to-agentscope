package com.example.dify2agentscope.domain.agentscope;

/**
 * AgentScope 规范化 workflow 的执行模式。
 * <p>DAG 模式用于 Dify 迁移后立即可用；REACT_SELF_LOOP 模式用于后续改造成更符合 AgentScope 的
 * 单 Agent / Harness Agent 自循环能力。</p>
 */
public enum AgentScopeExecutionMode {

    /** 保留节点图和边，按 DAG 编排执行，适合迁移兼容。 */
    DAG,

    /** 由 AgentScope ReActAgent/HarnessAgent 自己进行 reasoning/acting 自循环。 */
    REACT_SELF_LOOP,

    /** 多 Agent 或子 Agent 协作模式，后续对齐 Harness/Multi-Agent 能力。 */
    HARNESS_MULTI_AGENT
}
