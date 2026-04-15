package org.example.springboot.util;

/**
 * 业务单号格式：前缀 + yyyyMMdd(8) + 随机6位数字，共 2+14=16 位（与前端预览一致）
 */
public final class DocumentNoHelper {

    private DocumentNoHelper() {
    }

    public static boolean matchesPrefixedDateRandom(String no, String prefix) {
        if (no == null || prefix == null) {
            return false;
        }
        return no.matches("^" + java.util.regex.Pattern.quote(prefix) + "\\d{14}$");
    }
}
