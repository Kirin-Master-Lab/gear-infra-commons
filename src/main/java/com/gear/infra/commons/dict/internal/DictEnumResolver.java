package com.gear.infra.commons.dict.internal;

import com.gear.infra.commons.dict.BaseEnum;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典枚举编码与描述的公共解析器。
 *
 * <p>供实体字段转换和 Jackson 虚拟字段序列化共同使用，统一处理单值、
 * 逗号分隔多值以及未知编码。</p>
 */
/**
 * 字典解析内部实现，不建议业务代码直接依赖。
 */
public final class DictEnumResolver {

    /**
     * 缓存枚举编码到描述的映射，避免每次解析线性遍历全部枚举常量。
     */
    private static final Map<Class<?>, Map<String, String>> ENUM_DESCRIPTION_CACHE =
            new ConcurrentHashMap<Class<?>, Map<String, String>>();

    private DictEnumResolver() {
    }

    /**
     * 根据字典枚举解析编码对应的描述。
     *
     * @param enumClass 字典枚举类型
     * @param code 字典编码
     * @return 字典描述；未匹配时保留原编码，编码为 {@code null} 时返回 {@code null}
     */
    public static String resolve(Class<? extends BaseEnum<?>> enumClass, Object code) {
        if (code == null) {
            return null;
        }

        Map<String, String> descriptions = ENUM_DESCRIPTION_CACHE.computeIfAbsent(enumClass,
                DictEnumResolver::buildDescriptions);

        String codeValue = String.valueOf(code);
        if (!codeValue.contains(",")) {
            return resolveSingle(descriptions, codeValue);
        }

        StringBuilder resolvedDescriptions = new StringBuilder();
        for (String itemCode : codeValue.split(",")) {
            String trimmedCode = itemCode.trim();
            if (trimmedCode.isEmpty()) {
                continue;
            }
            if (resolvedDescriptions.length() > 0) {
                resolvedDescriptions.append(",");
            }
            resolvedDescriptions.append(resolveSingle(descriptions, trimmedCode));
        }
        return resolvedDescriptions.toString();
    }

    private static Map<String, String> buildDescriptions(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return Collections.emptyMap();
        }

        Map<String, String> descriptions = new HashMap<String, String>();
        for (Object constant : constants) {
            if (constant instanceof BaseEnum) {
                BaseEnum<?> baseEnum = (BaseEnum<?>) constant;
                descriptions.putIfAbsent(String.valueOf(baseEnum.getCode()), baseEnum.getDesc());
            }
        }
        return Collections.unmodifiableMap(descriptions);
    }

    private static String resolveSingle(Map<String, String> descriptions, String codeValue) {
        return descriptions.containsKey(codeValue) ? descriptions.get(codeValue) : codeValue;
    }
}
