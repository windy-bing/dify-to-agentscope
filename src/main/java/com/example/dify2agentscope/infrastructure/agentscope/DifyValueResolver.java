package com.example.dify2agentscope.infrastructure.agentscope;

import com.example.dify2agentscope.domain.workflow.WorkflowExecutionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 Dify 模板表达式和嵌套值，在工作流执行上下文中完成变量替换。
 * <p>
 * Resolve Dify template expressions and nested values, performing variable substitution in the workflow execution context.
 */
public class DifyValueResolver {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{#([^#]+)#}}");

    /**
     * 递归解析值，遇到 Map / List 深入展开，遇到字符串执行模板渲染。
     * <p>
     * Recursively resolve values, deeply expanding Map/List and rendering String templates.
     *
     * @param value   待解析的原始值 / raw value to resolve
     * @param context 工作流执行上下文 / workflow execution context
     * @return 解析后的值 / resolved value
     */
    public Object resolve(Object value, WorkflowExecutionContext context) {
        if (value instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if (map.containsKey("value") && (type != null || map.containsKey("auto"))) {
                return resolve(map.get("value"), context);
            }
            Map<String, Object> resolved = new LinkedHashMap<>();
            map.forEach((key, item) -> resolved.put(String.valueOf(key), resolve(item, context)));
            return resolved;
        }
        if (value instanceof List<?> list) {
            if (looksLikeSelector(list)) {
                return context.resolve(list);
            }
            List<Object> resolved = new ArrayList<>();
            for (Object item : list) {
                resolved.add(resolve(item, context));
            }
            return resolved;
        }
        if (value instanceof String text) {
            return render(text, context);
        }
        return value;
    }

    /**
     * 解析工具参数字典，返回扁平化的字符串键值映射。
     * <p>
     * Resolve tool parameters and return a flattened string-keyed map.
     *
     * @param parameters 工具参数 / tool parameters
     * @param context    工作流执行上下文 / workflow execution context
     * @return 解析后的参数映射 / resolved parameter map
     */
    public Map<String, Object> resolveToolParameters(Object parameters, WorkflowExecutionContext context) {
        Object resolved = resolve(parameters, context);
        if (!(resolved instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> flattened = new LinkedHashMap<>();
        map.forEach((key, value) -> flattened.put(String.valueOf(key), value));
        return flattened;
    }

    /**
     * 渲染字符串模板，将 {{#key.subKey#}} 替换为上下文中的真实值。
     * <p>
     * Render string template, replacing {{#key.subKey#}} with real values from context.
     *
     * @param template 包含占位符的模板字符串 / template string containing placeholders
     * @param context  工作流执行上下文 / workflow execution context
     * @return 渲染后的字符串 / rendered string
     */
    private String render(String template, WorkflowExecutionContext context) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String[] parts = matcher.group(1).split("\\.");
            Object value = parts.length >= 2 ? context.resolve(List.of(parts[0], parts[1])) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 判断列表是否为上下文选择器（格式如 ["sys", "key"] 或 ["conversation", "key"]）。
     * <p>
     * Determine if the list is a context selector (e.g. ["sys", "key"] or ["conversation", "key"]).
     *
     * @param list 待检查的列表 / list to check
     * @return 如果是选择器则返回 true / true if it is a selector
     */
    private boolean looksLikeSelector(List<?> list) {
        if (list.size() != 2) {
            return false;
        }
        Object first = list.get(0);
        return first instanceof String text
                && ("sys".equals(text) || "conversation".equals(text) || text.matches("\\d+"));
    }
}
