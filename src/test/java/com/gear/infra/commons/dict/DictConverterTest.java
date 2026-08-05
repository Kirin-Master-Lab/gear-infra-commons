package com.gear.infra.commons.dict;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DictConverterTest {

    @Test
    void convertsSingleMultipleUnknownAndNullCodes() {
        DictDto single = new DictDto(1);
        DictDto multiple = new DictDto("1, 2,9");
        DictDto unknown = new DictDto(9);
        DictDto nullCode = new DictDto(null);

        DictConverter.convert(Arrays.asList(single, multiple, unknown, nullCode));

        assertAll(
                () -> assertEquals("Enabled", single.statusName),
                () -> assertEquals("Enabled,Disabled,9", multiple.statusName),
                () -> assertEquals("9", unknown.statusName),
                () -> assertNull(nullCode.statusName)
        );
    }

    @Test
    void convertsNestedWrappersCollectionsMapsArraysAndInheritedFields() {
        ChildDto inherited = new ChildDto(2);
        DictDto inArray = new DictDto(1);
        DictDto inCollection = new DictDto(2);
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("array", new DictDto[]{inArray});
        values.put("collection", Collections.singletonList(inCollection));
        ResponseWrapper response = new ResponseWrapper(new ResponseWrapper(values), inherited);

        DictConverter.convert(response);

        assertAll(
                () -> assertEquals("Disabled", inherited.getStatusName()),
                () -> assertEquals("Enabled", inArray.statusName),
                () -> assertEquals("Disabled", inCollection.statusName)
        );
    }

    @Test
    void handlesCyclesAndIgnoresNonStringTargetFields() {
        CyclicDto first = new CyclicDto(1);
        CyclicDto second = new CyclicDto(2);
        first.next = second;
        second.next = first;
        InvalidTargetDto invalidTarget = new InvalidTargetDto();

        assertAll(
                () -> assertDoesNotThrow(() -> DictConverter.convert(first)),
                () -> assertDoesNotThrow(() -> DictConverter.convert(invalidTarget)),
                () -> assertEquals("Enabled", first.getStatusName()),
                () -> assertEquals("Disabled", second.getStatusName()),
                () -> assertEquals(Integer.valueOf(10), invalidTarget.statusName)
        );
    }

    @Test
    void clearsExistingDescriptionWhenCodeIsNull() {
        ReusableDto dto = new ReusableDto(null, "Previous value");

        DictConverter.convert(dto);

        assertNull(dto.statusName);
    }

    @Test
    void doesNotTraverseTransientFields() {
        DictDto included = new DictDto(1);
        DictDto ignored = new DictDto(2);

        DictConverter.convert(new TransientWrapper(included, ignored));

        assertEquals("Enabled", included.statusName);
        assertNull(ignored.statusName);
    }

    private enum StatusEnum implements BaseEnum<Integer> {
        ENABLED(1, "Enabled"),
        DISABLED(2, "Disabled");

        private final Integer code;
        private final String desc;

        StatusEnum(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public Integer getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    private static class DictDto {
        private final Object status;

        @ConvertDict(sourceField = "status", sourceClass = StatusEnum.class)
        private String statusName;

        DictDto(Object status) {
            this.status = status;
        }

        String getStatusName() {
            return statusName;
        }
    }

    private static class ChildDto extends DictDto {
        ChildDto(Object status) {
            super(status);
        }
    }

    private static class ResponseWrapper {
        private final Object data;
        private final Object extra;

        ResponseWrapper(Object data) {
            this(data, null);
        }

        ResponseWrapper(Object data, Object extra) {
            this.data = data;
            this.extra = extra;
        }
    }

    private static class CyclicDto extends DictDto {
        private CyclicDto next;

        CyclicDto(Object status) {
            super(status);
        }
    }

    private static class InvalidTargetDto {
        private final Integer status = 1;

        @ConvertDict(sourceField = "status", sourceClass = StatusEnum.class)
        private Integer statusName = 10;
    }

    private static class ReusableDto {
        private final Object status;

        @ConvertDict(sourceField = "status", sourceClass = StatusEnum.class)
        private String statusName;

        ReusableDto(Object status, String statusName) {
            this.status = status;
            this.statusName = statusName;
        }
    }

    private static class TransientWrapper {
        private final DictDto included;
        private final transient DictDto ignored;

        TransientWrapper(DictDto included, DictDto ignored) {
            this.included = included;
            this.ignored = ignored;
        }
    }
}
