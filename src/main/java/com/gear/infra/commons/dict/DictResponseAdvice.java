package com.gear.infra.commons.dict;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 接口响应体的字典转换处理器。
 *
 * <p>只处理 Controller 类或方法上标记了 {@link NeedDictConvert} 的请求。
 * 该处理器直接递归转换完整响应体，因此不依赖具体公司的统一响应包装类型。</p>
 */
@RestControllerAdvice
public class DictResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断当前接口是否需要进行字典转换。
     *
     * @param returnType Controller 返回值方法信息
     * @param converterType 响应消息转换器类型
     * @return 类或方法存在 {@link NeedDictConvert} 时返回 {@code true}
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(NeedDictConvert.class)
                || returnType.getContainingClass().isAnnotationPresent(NeedDictConvert.class);
    }

    /**
     * 在响应体序列化前填充其中的字典描述字段。
     *
     * <p>转换过程直接修改响应对象中的描述字段，并返回原响应对象。</p>
     *
     * @param body 原始响应体
     * @param returnType Controller 返回值方法信息
     * @param selectedContentType 响应媒体类型
     * @param selectedConverterType 实际使用的消息转换器类型
     * @param request 当前请求
     * @param response 当前响应
     * @return 完成字典转换后的原响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        DictConverter.convert(body);
        if (isJson(selectedContentType) && request instanceof ServletServerHttpRequest) {
            DictSerializationContext.enable(((ServletServerHttpRequest) request).getServletRequest());
        }
        return body;
    }

    /**
     * 判断当前响应是否由 JSON 序列化器处理。
     */
    private boolean isJson(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
                || mediaType.getSubtype().endsWith("+json");
    }
}
