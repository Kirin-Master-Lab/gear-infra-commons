package com.gear.infra.commons.dict;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictResponseAdviceTest {

    private final DictResponseAdvice advice = new DictResponseAdvice();

    @Test
    void supportsMethodAndControllerAnnotationsOnly() throws Exception {
        assertTrue(advice.supports(returnType(MethodAnnotatedController.class, "converted"), converterType()));
        assertTrue(advice.supports(returnType(TypeAnnotatedController.class, "converted"), converterType()));
        assertFalse(advice.supports(returnType(PlainController.class, "plain"), converterType()));
    }

    private static MethodParameter returnType(Class<?> type, String methodName) throws Exception {
        Method method = type.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends HttpMessageConverter<?>> converterType() {
        return (Class<? extends HttpMessageConverter<?>>) (Class<?>) HttpMessageConverter.class;
    }

    private static class MethodAnnotatedController {
        @NeedDictConvert
        public Object converted() {
            return null;
        }
    }

    @NeedDictConvert
    private static class TypeAnnotatedController {
        public Object converted() {
            return null;
        }
    }

    private static class PlainController {
        public Object plain() {
            return null;
        }
    }
}
