package com.example.dify2agentscope.domain.agentscope;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AgentScope 官方能力清单查询入口。
 * <p>运行时和文档都可以通过该类拿到同一份能力表，避免 README、代码注释和实现状态长期漂移。</p>
 */
public final class AgentScopeCapabilityManifest {

    /**
     * 工具类禁止实例化。
     */
    private AgentScopeCapabilityManifest() {
    }

    /**
     * 获取全部官方能力与项目自定义能力条目。
     *
     * @return 能力条目列表
     */
    public static List<AgentScopeOfficialCapability> all() {
        return List.of(AgentScopeOfficialCapability.values());
    }

    /**
     * 按当前落地状态分组能力条目。
     *
     * @return 状态 → 能力条目列表
     */
    public static Map<AgentScopeCapabilityStatus, List<AgentScopeOfficialCapability>> groupedByStatus() {
        return Arrays.stream(AgentScopeOfficialCapability.values())
                .collect(Collectors.groupingBy(
                        AgentScopeOfficialCapability::status,
                        Collectors.toList()));
    }

    /**
     * 判断当前项目是否已经直接使用或保留了某个官方能力的最小扩展点。
     *
     * @param capability 官方能力
     * @return true 表示已纳入当前项目能力清单
     */
    public static boolean isReserved(AgentScopeOfficialCapability capability) {
        return capability != null && capability.status() != AgentScopeCapabilityStatus.PROJECT_DEFINED;
    }
}
