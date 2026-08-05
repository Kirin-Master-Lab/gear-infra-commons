package com.gear.infra.commons.dict.jackson;

import com.gear.infra.commons.dict.DictCode;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 扫描 {@link DictCode} 并为编码属性追加字典描述虚拟属性。
 */
final class DictBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDescription,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>(beanProperties);
        Set<String> existingNames = new HashSet<String>();
        for (BeanPropertyWriter property : beanProperties) {
            existingNames.add(property.getName());
        }

        for (BeanPropertyWriter property : beanProperties) {
            DictCode annotation = property.getAnnotation(DictCode.class);
            if (annotation == null) {
                continue;
            }

            String textField = annotation.textField().trim();
            if (textField.isEmpty()) {
                textField = property.getName() + "Txt";
            }

            // 真实属性优先，避免同名 JSON 字段被虚拟属性重复输出。
            if (!existingNames.add(textField)) {
                continue;
            }
            result.add(new DictTextBeanPropertyWriter(property, textField, annotation.sourceClass()));
        }
        return result;
    }
}
