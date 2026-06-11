package com.gear.infra.commons.dict;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入字典描述的目标字段。
 *
 * <p>注解应添加在 {@link String} 类型的描述字段上，通过 {@link #sourceField()}
 * 指定同一对象中的编码字段，通过 {@link #sourceClass()} 指定字典枚举。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConvertDict {

    /**
     * 指定当前对象中存放字典编码的字段名称。
     *
     * @return 编码字段名称
     */
    String sourceField();

    /**
     * 指定用于将编码转换为描述的字典枚举。
     *
     * @return 实现 {@link BaseEnum} 的枚举类型
     */
    Class<? extends BaseEnum<?>> sourceClass();
}
