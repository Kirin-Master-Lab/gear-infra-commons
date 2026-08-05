package com.gear.infra.commons.dict.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gear.infra.commons.dict.jackson.DictJacksonModule;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 字典虚拟字段的 Jackson 自动配置。
 *
 * <p>仅在 Servlet Web 应用且存在 Jackson 时注册 {@link DictJacksonModule}，
 * 并在 Spring Boot 创建 ObjectMapper 前提供该模块。</p>
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ObjectMapper.class)
public class DictJacksonAutoConfiguration {

    /**
     * 注册字典虚拟字段 Jackson 模块。
     *
     * @return 字典 Jackson 模块
     */
    @Bean
    @ConditionalOnMissingBean(DictJacksonModule.class)
    public DictJacksonModule dictJacksonModule() {
        return new DictJacksonModule();
    }
}
