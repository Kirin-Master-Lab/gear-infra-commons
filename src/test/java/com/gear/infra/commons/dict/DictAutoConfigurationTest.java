package com.gear.infra.commons.dict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
        assertTrue(configurations.contains(DictJacksonAutoConfiguration.class.getName()));
    }

    @Test
    void createsAdviceInServletWebApplication() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(DictAutoConfiguration.class, DictJacksonAutoConfiguration.class);

        try {
            context.refresh();
            assertEquals(1, context.getBeansOfType(DictResponseAdvice.class).size());
            assertEquals(1, context.getBeansOfType(DictJacksonModule.class).size());
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
    void backsOffWhenApplicationProvidesJacksonModule() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(CustomModuleConfiguration.class, DictJacksonAutoConfiguration.class);

        try {
            context.refresh();
            assertSame(CustomModuleConfiguration.CUSTOM_MODULE,
                    context.getBean(DictJacksonModule.class));
        } finally {
            context.close();
        }
    }

    @Test
    void bootObjectMapperRegistersDictJacksonModule() throws Exception {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(DictJacksonAutoConfiguration.class, JacksonAutoConfiguration.class);

        try {
            context.refresh();
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
            DictSerializationContext.enable(request);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            JsonNode json = objectMapper.readTree(
                    objectMapper.writeValueAsString(new AutoConfiguredDto("1")));

            assertEquals("Enabled", json.get("statusTxt").asText());
        } finally {
            RequestContextHolder.resetRequestAttributes();
            context.close();
        }
    }

    @Test
    void doesNotCreateAdviceOutsideWebApplication() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DictAutoConfiguration.class, DictJacksonAutoConfiguration.class);

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

    @Configuration(proxyBeanMethods = false)
    static class CustomModuleConfiguration {
        private static final DictJacksonModule CUSTOM_MODULE = new DictJacksonModule();

        @Bean
        DictJacksonModule customDictJacksonModule() {
            return CUSTOM_MODULE;
        }
    }

    private enum AutoConfiguredStatus implements BaseEnum<String> {
        ENABLED("1", "Enabled");

        private final String code;
        private final String desc;

        AutoConfiguredStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    private static class AutoConfiguredDto {
        @DictCode(sourceClass = AutoConfiguredStatus.class)
        private final String status;

        AutoConfiguredDto(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}
