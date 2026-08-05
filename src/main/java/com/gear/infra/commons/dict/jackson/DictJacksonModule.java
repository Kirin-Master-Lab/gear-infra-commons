package com.gear.infra.commons.dict.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 注册字典虚拟字段序列化能力的 Jackson 模块。
 *
 * <p>Spring Boot 会自动将该 Module Bean 注册到其管理的 ObjectMapper。</p>
 */
public class DictJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public DictJacksonModule() {
        super(DictJacksonModule.class.getName());
        setSerializerModifier(new DictBeanSerializerModifier());
    }
}
