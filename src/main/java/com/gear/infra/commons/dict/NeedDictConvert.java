package com.gear.infra.commons.dict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启接口响应字典转换。
 *
 * <p>可标记在 Controller 类或方法上。被标记的接口在写出响应体前，
 * 将由 {@link DictResponseAdvice} 调用 {@link DictConverter} 完成实体描述字段填充，
 * 并为 JSON 序列化启用 {@link DictCode} 虚拟描述字段。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedDictConvert {
}
