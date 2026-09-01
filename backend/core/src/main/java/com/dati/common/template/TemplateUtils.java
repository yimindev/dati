package com.dati.common.template;

import java.util.Collection;

/**
 * Package-private utilities shared by renderer implementations.
 */
final class TemplateUtils {

    private TemplateUtils() {}

    /**
     * 判断值对 {{@code #if}} 是否为 truthy。
     * falsy: null、空字符串、空集合、空数组。
     * 0 和 false 保留为 truthy（SQL 中有实际语义）。
     */
    static boolean isTruthy(Object value) {
        switch (value) {
            case null -> {
                return false;
            }
            case String s when s.isEmpty() -> {
                return false;
            }
            case Collection<?> c when c.isEmpty() -> {
                return false;
            }
            default -> {
            }
        }
        return !value.getClass().isArray() || java.lang.reflect.Array.getLength(value) != 0;
    }
}
