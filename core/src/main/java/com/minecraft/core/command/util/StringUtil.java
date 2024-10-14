package com.minecraft.core.command.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author The Apache Software Foundation
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtil {

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return startsWith(str, prefix);
    }

    private static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) return (str == null && prefix == null);
        if (prefix.length() > str.length()) return false;

        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static int countMatches(String str, String sub) {
        if (isEmpty(str) || isEmpty(sub)) return 0;

        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }

        return count;
    }

    public static String uncapitalize(String str) {
        if (str == null || str.length() == 0) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
