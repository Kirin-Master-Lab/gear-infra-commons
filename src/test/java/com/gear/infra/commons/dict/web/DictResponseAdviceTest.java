package com.gear.infra.commons.dict.web;

import com.gear.infra.commons.dict.NeedDictConvert;
import com.gear.infra.commons.dict.internal.DictSerializationContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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

    @Test
    void enablesVirtualFieldsForJsonServletResponseOnly() {
        MockHttpServletRequest jsonRequest = new MockHttpServletRequest();
        advice.beforeBodyWrite(null, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(jsonRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        MockHttpServletRequest problemJsonRequest = new MockHttpServletRequest();
        advice.beforeBodyWrite(null, null, MediaType.valueOf("application/problem+json"), null,
                new ServletServerHttpRequest(problemJsonRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        MockHttpServletRequest textRequest = new MockHttpServletRequest();
        advice.beforeBodyWrite(null, null, MediaType.TEXT_PLAIN, null,
                new ServletServerHttpRequest(textRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertTrue(Boolean.TRUE.equals(jsonRequest.getAttribute(DictSerializationContext.REQUEST_ATTRIBUTE)));
        assertTrue(Boolean.TRUE.equals(problemJsonRequest.getAttribute(DictSerializationContext.REQUEST_ATTRIBUTE)));
        assertFalse(Boolean.TRUE.equals(textRequest.getAttribute(DictSerializationContext.REQUEST_ATTRIBUTE)));
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
