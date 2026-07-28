package com.earthy.shop.support;

import java.lang.reflect.Field;

public final class TestEntityUtils {

    private TestEntityUtils() {
    }

    public static void setId(Object target, Long id) {
        setField(target, "id", id);
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("테스트 엔티티 필드 설정 실패", e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        throw new IllegalArgumentException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
