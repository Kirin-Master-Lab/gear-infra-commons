package com.gear.infra.commons.dict;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 字典转换功能的 Spring Boot 2.x 自动配置。
 *
 * <p>仅在 Servlet Web 应用且存在 {@link ResponseBodyAdvice} 时生效。
 * 当主工程没有自定义 {@link DictResponseAdvice} Bean 时，自动注册默认实现。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseBodyAdvice.class)
public class DictAutoConfiguration {

    /**
     * 注册默认的响应字典转换处理器。
     *
     * @return 响应字典转换处理器
     */
    @Bean
    @ConditionalOnMissingBean(DictResponseAdvice.class)
    public DictResponseAdvice dictResponseAdvice() {
        return new DictResponseAdvice();
    }
}
