package com.dati.common;

import org.springframework.util.ObjectUtils;

public class StringUtils {

    public static boolean isEmpty(String str) {
        return ObjectUtils.isEmpty(str);
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

}
