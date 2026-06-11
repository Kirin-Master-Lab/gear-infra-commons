package com.gear.infra.commons.dict;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

/**
 * 根据编码属性动态输出字典描述的 Jackson 虚拟属性写入器。
 */
final class DictTextBeanPropertyWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = 1L;

    private final Class<? extends BaseEnum<?>> enumClass;

    DictTextBeanPropertyWriter(BeanPropertyWriter sourceWriter, String textField,
                               Class<? extends BaseEnum<?>> enumClass) {
        super(sourceWriter, new PropertyName(textField));
        this.enumClass = enumClass;
    }

    /**
     * 仅在当前请求启用字典转换时输出虚拟字段。
     *
     * <p>该写入器继承原编码属性的 null inclusion 配置，因此编码为 null 时，
     * 虚拟字段遵循应用现有 Jackson 空值输出策略。</p>
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator generator,
                                 SerializerProvider provider) throws Exception {
        if (!DictSerializationContext.isEnabled()) {
            return;
        }

        String description = DictEnumResolver.resolve(enumClass, get(bean));
        if (description == null && _suppressNulls) {
            return;
        }

        generator.writeFieldName(_name);
        if (description == null) {
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, generator, provider);
            } else {
                provider.defaultSerializeNull(generator);
            }
            return;
        }
        provider.defaultSerializeValue(description, generator);
    }
}
