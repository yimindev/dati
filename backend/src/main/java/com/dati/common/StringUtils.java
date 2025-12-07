package com.dati.common;

import org.springframework.util.ObjectUtils;

public class StringUtils {

    public static boolean isBlank(String str) {
        return ObjectUtils.isEmpty(str);
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

}
