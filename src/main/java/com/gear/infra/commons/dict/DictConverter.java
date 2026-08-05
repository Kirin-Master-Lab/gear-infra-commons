package com.gear.infra.commons.dict;

import com.gear.infra.commons.dict.internal.DictEnumResolver;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于反射的字典描述转换工具。
 *
 * <p>从响应对象开始递归遍历普通对象、集合、Map 和数组，找到带有
 * {@link ConvertDict} 的 String 字段后，根据指定枚举和源编码字段写入描述。
 * 转换过程支持继承字段、多值编码和循环引用对象。</p>
 */
public class DictConverter {

    private static final Logger LOGGER = Logger.getLogger(DictConverter.class.getName());

    /**
     * 按运行时类型缓存字段访问计划，避免每个响应对象重复扫描继承层级和查找源字段。
     */
    private static final Map<Class<?>, List<FieldMetadata>> FIELD_CACHE =
            new ConcurrentHashMap<Class<?>, List<FieldMetadata>>();

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

        for (FieldMetadata fieldMetadata : getFieldMetadata(objectClass)) {
            convertField(obj, fieldMetadata, visited);
        }
    }

    /**
     * 转换单个字典描述字段；普通字段则继续向下递归。
     */
    private static void convertField(Object obj, FieldMetadata fieldMetadata, Set<Object> visited) {
        try {
            if (fieldMetadata.isDictField()) {
                Object codeValue = fieldMetadata.sourceField.get(obj);
                // 即使编码为 null 也覆盖原描述，避免复用 DTO 时遗留旧值。
                fieldMetadata.field.set(obj,
                        DictEnumResolver.resolve(fieldMetadata.enumClass, codeValue));
                return;
            }
            convert(fieldMetadata.field.get(obj), visited);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
            LOGGER.log(Level.FINE, "Skip dictionary conversion for inaccessible field: "
                    + fieldMetadata.field, ex);
        }
    }

    private static List<FieldMetadata> getFieldMetadata(Class<?> objectClass) {
        return FIELD_CACHE.computeIfAbsent(objectClass, DictConverter::buildFieldMetadata);
    }

    /**
     * 构建当前运行时类型的字段访问计划，并在缓存阶段校验字典字段配置。
     */
    private static List<FieldMetadata> buildFieldMetadata(Class<?> objectClass) {
        List<FieldMetadata> result = new ArrayList<FieldMetadata>();
        Class<?> currentClass = objectClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }

                ConvertDict annotation = field.getAnnotation(ConvertDict.class);
                if (annotation == null) {
                    addFieldMetadata(result, field, null, null);
                    continue;
                }
                if (field.getType() != String.class) {
                    logInvalidConfiguration(field, "the target field must be String");
                    continue;
                }

                Field sourceField = getField(objectClass, annotation.sourceField());
                if (sourceField == null) {
                    logInvalidConfiguration(field, "source field '" + annotation.sourceField() + "' was not found");
                    continue;
                }
                addFieldMetadata(result, field, sourceField, annotation.sourceClass());
            }
            currentClass = currentClass.getSuperclass();
        }
        return Collections.unmodifiableList(result);
    }

    private static void addFieldMetadata(List<FieldMetadata> result, Field field, Field sourceField,
                                         Class<? extends BaseEnum<?>> enumClass) {
        try {
            makeAccessible(field);
            if (sourceField != null) {
                makeAccessible(sourceField);
            }
            result.add(new FieldMetadata(field, sourceField, enumClass));
        } catch (SecurityException ex) {
            LOGGER.log(Level.FINE, "Skip dictionary conversion for inaccessible field: " + field, ex);
        }
    }

    private static void makeAccessible(Field field) {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    private static void logInvalidConfiguration(Field field, String reason) {
        LOGGER.fine("Ignore @ConvertDict on " + field + ": " + reason);
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

    private static final class FieldMetadata {

        private final Field field;
        private final Field sourceField;
        private final Class<? extends BaseEnum<?>> enumClass;

        private FieldMetadata(Field field, Field sourceField, Class<? extends BaseEnum<?>> enumClass) {
            this.field = field;
            this.sourceField = sourceField;
            this.enumClass = enumClass;
        }

        private boolean isDictField() {
            return sourceField != null;
        }
    }
}
