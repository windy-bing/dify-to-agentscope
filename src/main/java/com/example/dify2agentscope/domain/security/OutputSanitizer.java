package com.example.dify2agentscope.domain.security;

/**
 * 输出脱敏器。
 * <p>
 * 用于对工作流执行结果进行脱敏处理，防止敏感信息（如邮箱、手机号、身份证号）泄漏。
 * Output sanitizer — sanitizes workflow execution output to prevent leakage of sensitive
 * information such as email addresses, phone numbers, and ID numbers.
 */
public class OutputSanitizer {

    /** 最大输出长度 / Maximum output length */
    private final int maxOutputLength;

    /**
     * 使用默认最大输出长度（8000）构造脱敏器。
     * Creates a sanitizer with the default maximum output length (8000).
     */
    public OutputSanitizer() {
        this(8000);
    }

    /**
     * 使用指定的最大输出长度构造脱敏器。
     * Creates a sanitizer with the specified maximum output length.
     *
     * @param maxOutputLength 最大输出长度 / maximum output length
     */
    public OutputSanitizer(int maxOutputLength) {
        this.maxOutputLength = maxOutputLength;
    }

    /**
     * 对输入字符串执行脱敏操作。
     * <p>
     * 替换其中的邮箱、手机号、身份证号为占位符，并在超过最大长度时截断。
     * Sanitizes the input string by replacing email addresses, phone numbers, and ID numbers
     * with placeholders, and truncates it if it exceeds the maximum length.
     *
     * @param value 原始输入值 / raw input value
     * @return 脱敏后的字符串 / sanitized string
     */
    public String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[已脱敏]")
                .replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)", "[已脱敏]")
                .replaceAll("(?<!\\d)\\d{17}[0-9Xx](?!\\d)", "[已脱敏]");
        if (sanitized.length() > maxOutputLength) {
            sanitized = sanitized.substring(0, maxOutputLength);
        }
        return sanitized;
    }
}
