package com.gear.infra.commons.dict.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gear.infra.commons.dict.BaseEnum;
import com.gear.infra.commons.dict.DictCode;
import com.gear.infra.commons.dict.web.DictResponseAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictJacksonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new DictJacksonModule());

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writesMultipleDefaultAndCustomVirtualFieldsWhenRequestIsEnabled() throws Exception {
        enableJsonRequest();
        MultiDictDto dto = new MultiDictDto("1", "2", "9");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertEquals("1", json.get("keyword1").asText());
        assertEquals("Enabled", json.get("keyword1Txt").asText());
        assertEquals("Disabled", json.get("keyword2Txt").asText());
        assertEquals("9", json.get("keywordLabel").asText());
    }

    @Test
    void writesVirtualFieldsForNestedObjectsCollectionsAndArrays() throws Exception {
        enableJsonRequest();
        Wrapper wrapper = new Wrapper(
                new MultiDictDto("1", "2", "1, 2,9"),
                Arrays.asList(new MultiDictDto("2", "1", "1")),
                new MultiDictDto[]{new MultiDictDto("1", "1", "2")});

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(wrapper));

        assertEquals("Enabled", json.at("/data/keyword1Txt").asText());
        assertEquals("Disabled", json.at("/items/0/keyword1Txt").asText());
        assertEquals("Enabled", json.at("/array/0/keyword1Txt").asText());
        assertEquals("Enabled,Disabled,9", json.at("/data/keywordLabel").asText());
    }

    @Test
    void doesNotWriteVirtualFieldWithoutEnabledRequest() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(new MultiDictDto("1", "2", "1")));

        assertFalse(json.has("keyword1Txt"));
        assertFalse(json.has("keyword2Txt"));
        assertFalse(json.has("keywordLabel"));
    }

    @Test
    void realPropertyWinsWhenVirtualFieldNameConflicts() throws Exception {
        enableJsonRequest();

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(new ExistingTextDto("1", "Manual")));

        assertEquals("Manual", json.get("statusTxt").asText());
    }

    @Test
    void nullVirtualFieldFollowsObjectMapperInclusion() throws Exception {
        enableJsonRequest();

        JsonNode defaultJson = objectMapper.readTree(
                objectMapper.writeValueAsString(new NullableDto(null)));
        assertTrue(defaultJson.has("statusTxt"));
        assertNull(defaultJson.get("statusTxt").textValue());

        ObjectMapper nonNullMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new DictJacksonModule());
        JsonNode nonNullJson = nonNullMapper.readTree(
                nonNullMapper.writeValueAsString(new NullableDto(null)));
        assertFalse(nonNullJson.has("status"));
        assertFalse(nonNullJson.has("statusTxt"));
    }

    private static void enableJsonRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        new DictResponseAdvice().beforeBodyWrite(
                null, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()));
    }

    private enum StatusEnum implements BaseEnum<String> {
        ENABLED("1", "Enabled"),
        DISABLED("2", "Disabled");

        private final String code;
        private final String desc;

        StatusEnum(String code, String desc) {
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

    private static class MultiDictDto {
        @DictCode(sourceClass = StatusEnum.class)
        private final String keyword1;

        @DictCode(sourceClass = StatusEnum.class)
        private final String keyword2;

        @DictCode(sourceClass = StatusEnum.class, textField = "keywordLabel")
        private final String keyword3;

        MultiDictDto(String keyword1, String keyword2, String keyword3) {
            this.keyword1 = keyword1;
            this.keyword2 = keyword2;
            this.keyword3 = keyword3;
        }

        public String getKeyword1() {
            return keyword1;
        }

        public String getKeyword2() {
            return keyword2;
        }

        public String getKeyword3() {
            return keyword3;
        }
    }

    private static class Wrapper {
        private final MultiDictDto data;
        private final Iterable<MultiDictDto> items;
        private final MultiDictDto[] array;

        Wrapper(MultiDictDto data, Iterable<MultiDictDto> items, MultiDictDto[] array) {
            this.data = data;
            this.items = items;
            this.array = array;
        }

        public MultiDictDto getData() {
            return data;
        }

        public Iterable<MultiDictDto> getItems() {
            return items;
        }

        public MultiDictDto[] getArray() {
            return array;
        }
    }

    private static class ExistingTextDto {
        @DictCode(sourceClass = StatusEnum.class)
        private final String status;
        private final String statusTxt;

        ExistingTextDto(String status, String statusTxt) {
            this.status = status;
            this.statusTxt = statusTxt;
        }

        public String getStatus() {
            return status;
        }

        public String getStatusTxt() {
            return statusTxt;
        }
    }

    private static class NullableDto {
        @DictCode(sourceClass = StatusEnum.class)
        private final String status;

        NullableDto(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}
