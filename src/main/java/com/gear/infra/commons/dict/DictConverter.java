package com.gear.infra.commons.dict;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 基于反射的字典描述转换工具。
 *
 * <p>从响应对象开始递归遍历普通对象、集合、Map 和数组，找到带有
 * {@link ConvertDict} 的 String 字段后，根据指定枚举和源编码字段写入描述。
 * 转换过程支持继承字段、多值编码和循环引用对象。</p>
 */
public class DictConverter {

    /**
     * 递归转换对象中所有标记了 {@link ConvertDict} 的字典描述字段。
     *
     * @param obj 待转换的响应对象，可以是普通对象、集合、Map 或数组
     */
    public static void convert(Object obj) {
        // 使用对象身份而不是 equals 判断是否访问过，避免循环引用引发无限递归。
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        convert(obj, visited);
    }

    /**
     * 根据对象实际类型选择容器遍历或普通字段扫描。
     */
    private static void convert(Object obj, Set<Object> visited) {
        if (obj == null) {
            return;
        }

        Class<?> objectClass = obj.getClass();
        boolean container = obj instanceof Collection || obj instanceof Map || objectClass.isArray();
        if ((!container && isSimpleValueType(objectClass)) || !visited.add(obj)) {
            return;
        }

        // 容器只递归处理其元素或值，不反射 JDK 容器内部字段。
        if (obj instanceof Collection) {
            for (Object item : (Collection<?>) obj) {
                convert(item, visited);
            }
            return;
        }

        if (obj instanceof Map) {
            for (Object value : ((Map<?, ?>) obj).values()) {
                convert(value, visited);
            }
            return;
        }

        if (objectClass.isArray()) {
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                convert(Array.get(obj, i), visited);
            }
            return;
        }

        // 逐级扫描父类字段，确保继承自基类的字典字段也能完成转换。
        Class<?> currentClass = objectClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                convertField(obj, objectClass, field, visited);
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * 转换单个字典描述字段；普通字段则继续向下递归。
     */
    private static void convertField(Object obj, Class<?> objectClass, Field field, Set<Object> visited) {
        try {
            field.setAccessible(true);
            ConvertDict annotation = field.getAnnotation(ConvertDict.class);
            if (annotation != null) {
                if (field.getType() != String.class) {
                    return;
                }
                Field sourceField = getField(objectClass, annotation.sourceField());
                if (sourceField == null) {
                    return;
                }
                sourceField.setAccessible(true);
                Object codeValue = sourceField.get(obj);
                if (codeValue != null) {
                    field.set(obj, DictEnumResolver.resolve(annotation.sourceClass(), codeValue));
                }
                return;
            }
            convert(field.get(obj), visited);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // 单个字段不可访问时跳过该字段，不能因此阻断整个接口响应。
        }
    }

    /**
     * 从当前类开始向父类查找指定字段。
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 判断是否为无需继续反射遍历的基础值类型。
     */
    private static boolean isSimpleValueType(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isEnum()) {
            return true;
        }
        Package valuePackage = clazz.getPackage();
        if (valuePackage == null) {
            return false;
        }
        String packageName = valuePackage.getName();
        return packageName.startsWith("java.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("sun.")
                || packageName.startsWith("com.sun.");
    }
}
