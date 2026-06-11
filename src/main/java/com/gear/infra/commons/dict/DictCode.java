package com.gear.infra.commons.dict;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要在 JSON 中追加字典描述虚拟字段的编码字段。
 *
 * <p>接口标记 {@link NeedDictConvert} 后，Jackson 会保留原编码字段，并在同级追加
 * 描述字段。描述字段默认命名为“编码字段 JSON 名称 + Txt”，也可通过
 * {@link #textField()} 自定义。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictCode {

    /**
     * 指定用于将编码转换为描述的字典枚举。
     *
     * @return 实现 {@link BaseEnum} 的枚举类型
     */
    Class<? extends BaseEnum<?>> sourceClass();

    /**
     * 指定虚拟描述字段的 JSON 名称。
     *
     * @return 描述字段名称；为空时默认使用“编码字段 JSON 名称 + Txt”
     */
    String textField() default "";
}
