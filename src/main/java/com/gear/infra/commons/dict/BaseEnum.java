package com.gear.infra.commons.dict;

/**
 * 字典枚举的统一接口。
 *
 * <p>业务枚举实现该接口后，即可由 {@link DictConverter} 根据字典编码查找对应描述。</p>
 *
 * @param <T> 字典编码的数据类型
 */
public interface BaseEnum<T>  {

    /**
     * 获取字典编码。
     *
     * @return 字典编码
     */
    T getCode();

    /**
     * 获取字典编码对应的展示描述。
     *
     * @return 字典描述
     */
    String getDesc();
}
