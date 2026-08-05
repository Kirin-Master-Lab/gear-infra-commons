package com.gear.infra.commons.dict.internal;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 字典虚拟字段的请求级开关。
 *
 * <p>开关存储在当前 Servlet 请求属性中，不使用 ThreadLocal 保存业务状态，
 * 请求结束后由 Servlet 容器统一释放。</p>
 */
/**
 * 字典序列化请求上下文内部实现，不建议业务代码直接依赖。
 */
public final class DictSerializationContext {

    public static final String REQUEST_ATTRIBUTE =
            DictSerializationContext.class.getName() + ".ENABLED";

    private DictSerializationContext() {
    }

    public static void enable(HttpServletRequest request) {
        request.setAttribute(REQUEST_ATTRIBUTE, Boolean.TRUE);
    }

    public static boolean isEnabled() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return false;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTRIBUTE));
    }
}
