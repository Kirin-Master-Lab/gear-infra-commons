package com.gear.infra.commons.dict;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictAutoConfigurationTest {

    @Test
    void exposesAutoConfigurationThroughSpringFactories() {
        List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
                EnableAutoConfiguration.class, getClass().getClassLoader());

        assertTrue(configurations.contains(DictAutoConfiguration.class.getName()));
    }

    @Test
    void createsAdviceInServletWebApplication() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(DictAutoConfiguration.class);

        try {
            context.refresh();
            assertEquals(1, context.getBeansOfType(DictResponseAdvice.class).size());
        } finally {
            context.close();
        }
    }

    @Test
    void backsOffWhenApplicationProvidesAdvice() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(CustomAdviceConfiguration.class, DictAutoConfiguration.class);

        try {
            context.refresh();
            assertSame(CustomAdviceConfiguration.CUSTOM_ADVICE,
                    context.getBean(DictResponseAdvice.class));
        } finally {
            context.close();
        }
    }

    @Test
    void doesNotCreateAdviceOutsideWebApplication() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DictAutoConfiguration.class);

        try {
            context.refresh();
            assertFalse(context.containsBean("dictResponseAdvice"));
        } finally {
            context.close();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAdviceConfiguration {
        private static final DictResponseAdvice CUSTOM_ADVICE = new DictResponseAdvice();

        @Bean
        DictResponseAdvice customDictResponseAdvice() {
            return CUSTOM_ADVICE;
        }
    }
}
