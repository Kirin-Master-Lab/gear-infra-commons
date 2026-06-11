package com.gear.infra.commons.dict;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典枚举编码与描述的公共解析器。
 *
 * <p>供实体字段转换和 Jackson 虚拟字段序列化共同使用，统一处理单值、
 * 逗号分隔多值以及未知编码。</p>
 */
final class DictEnumResolver {

    private static final Map<Class<?>, BaseEnum<?>[]> ENUM_CACHE =
            new ConcurrentHashMap<Class<?>, BaseEnum<?>[]>();

    private DictEnumResolver() {
    }

    /**
     * 根据字典枚举解析编码对应的描述。
     *
     * @param enumClass 字典枚举类型
     * @param code 字典编码
     * @return 字典描述；未匹配时保留原编码，编码为 {@code null} 时返回 {@code null}
     */
    static String resolve(Class<? extends BaseEnum<?>> enumClass, Object code) {
        if (code == null) {
            return null;
        }

        BaseEnum<?>[] constants = ENUM_CACHE.computeIfAbsent(enumClass,
                clazz -> (BaseEnum<?>[]) clazz.getEnumConstants());
        if (constants == null) {
            return String.valueOf(code);
        }

        String codeValue = String.valueOf(code);
        if (!codeValue.contains(",")) {
            return resolveSingle(constants, codeValue);
        }

        StringBuilder descriptions = new StringBuilder();
        for (String itemCode : codeValue.split(",")) {
            String trimmedCode = itemCode.trim();
            if (trimmedCode.isEmpty()) {
                continue;
            }
            if (descriptions.length() > 0) {
                descriptions.append(",");
            }
            descriptions.append(resolveSingle(constants, trimmedCode));
        }
        return descriptions.toString();
    }

    private static String resolveSingle(BaseEnum<?>[] constants, String codeValue) {
        for (BaseEnum<?> constant : constants) {
            if (codeValue.equals(String.valueOf(constant.getCode()))) {
                return constant.getDesc();
            }
        }
        return codeValue;
    }
}
