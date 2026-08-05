# 字典注解使用说明

## 1. 功能概述

`com.gear.infra.commons.dict` 提供接口响应字典转换能力，适用于将业务对象中的枚举编码转换为可展示的文本。

组件支持两种使用模式：

1. 推荐模式：在编码字段上使用 `@DictCode`，JSON 序列化时自动增加同级文本字段，不需要在 Java DTO 中声明 `Txt` 字段。
2. 兼容模式：在实体文本字段上使用 `@ConvertDict`，响应前将转换结果写入该字段。

两种模式都需要在 Controller 类或接口方法上添加 `@NeedDictConvert` 才会生效。

## 2. 环境要求

- JDK 8 或 JDK 17
- Spring Boot 2.x
- Spring MVC 5.3.x
- Jackson 2.13.x
- `javax.servlet` API

当前版本不支持 Spring Boot 3、Spring 6 和 `jakarta.servlet`。

## 3. 引入依赖

下游项目引入公共组件：

```xml
<dependency>
    <groupId>com.gear.infra</groupId>
    <artifactId>gear-infra-commons</artifactId>
    <version>1.0.3-SNAPSHOT</version>
</dependency>
```

Spring Boot 2.x 会通过 `spring.factories` 自动注册：

- `com.gear.infra.commons.dict.web.DictResponseAdvice`
- `com.gear.infra.commons.dict.jackson.DictJacksonModule`

主工程不需要额外扫描 `com.gear.infra.commons` 包，也不需要手动声明配置类。

### 3.1 包结构

根包仅保留业务代码需要直接使用的 API：

- `BaseEnum`
- `ConvertDict`
- `DictCode`
- `NeedDictConvert`
- `DictConverter`

其余实现按职责分层：

| 包 | 职责 | 是否建议业务代码直接使用 |
| --- | --- | --- |
| `dict.autoconfigure` | Spring Boot 自动配置 | 否，默认自动装配即可 |
| `dict.web` | Spring MVC 响应处理 | 否，除非需要自定义 Advice Bean |
| `dict.jackson` | Jackson 虚拟字段输出 | 仅手动注册 `ObjectMapper` 时使用 `DictJacksonModule` |
| `dict.internal` | 内部解析和请求上下文 | 否 |

### 3.2 旧包路径迁移

当前版本已移除根包下的实现类。若业务项目曾手动引用这些类型，请更新 import：

| 原类型 | 新类型 |
| --- | --- |
| `com.gear.infra.commons.dict.DictResponseAdvice` | `com.gear.infra.commons.dict.web.DictResponseAdvice` |
| `com.gear.infra.commons.dict.DictJacksonModule` | `com.gear.infra.commons.dict.jackson.DictJacksonModule` |
| `com.gear.infra.commons.dict.DictAutoConfiguration` | `com.gear.infra.commons.dict.autoconfigure.DictAutoConfiguration` |
| `com.gear.infra.commons.dict.DictJacksonAutoConfiguration` | `com.gear.infra.commons.dict.autoconfigure.DictJacksonAutoConfiguration` |

## 4. 定义字典枚举

字典枚举需要实现 `BaseEnum<T>`：

```java
public enum EmailKeywordEnum implements BaseEnum<String> {

    USER_NAME("USER_NAME", "用户名称"),
    EMAIL("EMAIL", "邮箱地址");

    private final String code;
    private final String desc;

    EmailKeywordEnum(String code, String desc) {
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
```

所需 import：

```java
import com.gear.infra.commons.dict.BaseEnum;
```

`BaseEnum<T>` 的泛型类型可以是 `String`、`Integer` 等常用编码类型。转换时组件会使用编码的字符串形式进行匹配。

## 5. 推荐模式：`@DictCode`

### 5.1 基础用法

只需要在编码字段上添加 `@DictCode`：

```java
public class EmailTemplateVO {

    /**
     * 关键字编码。
     */
    @DictCode(sourceClass = EmailKeywordEnum.class)
    private String keyword1;

    public String getKeyword1() {
        return keyword1;
    }

    public void setKeyword1(String keyword1) {
        this.keyword1 = keyword1;
    }
}
```

所需 import：

```java
import com.gear.infra.commons.dict.DictCode;
```

Controller 方法添加 `@NeedDictConvert`：

```java
@RestController
@RequestMapping("/email-template")
public class EmailTemplateController {

    @NeedDictConvert
    @GetMapping("/{id}")
    public EmailTemplateVO detail(@PathVariable Long id) {
        return emailTemplateService.getById(id);
    }
}
```

所需 import：

```java
import com.gear.infra.commons.dict.NeedDictConvert;
```

原始 Java 对象只包含：

```java
keyword1 = "USER_NAME"
```

接口 JSON 响应会自动增加 `keyword1Txt`：

```json
{
  "keyword1": "USER_NAME",
  "keyword1Txt": "用户名称"
}
```

Java DTO 中不需要声明 `keyword1Txt` 字段。

### 5.2 多个字典字段

DTO 中有多个枚举编码时，分别标记编码字段即可：

```java
public class EmailTemplateVO {

    @DictCode(sourceClass = EmailKeywordEnum.class)
    private String keyword1;

    @DictCode(sourceClass = EmailKeywordEnum.class)
    private String keyword2;

    @DictCode(sourceClass = EmailKeywordEnum.class)
    private String keyword3;
}
```

响应示例：

```json
{
  "keyword1": "USER_NAME",
  "keyword1Txt": "用户名称",
  "keyword2": "EMAIL",
  "keyword2Txt": "邮箱地址",
  "keyword3": "UNKNOWN",
  "keyword3Txt": "UNKNOWN"
}
```

### 5.3 自定义文本字段名称

默认文本字段名为：

```text
编码字段的 JSON 名称 + Txt
```

可通过 `textField` 指定其他名称：

```java
@DictCode(
        sourceClass = EmailKeywordEnum.class,
        textField = "keywordName"
)
private String keyword;
```

响应示例：

```json
{
  "keyword": "USER_NAME",
  "keywordName": "用户名称"
}
```

### 5.4 类级统一开启

如果一个 Controller 中的所有接口都需要字典转换，可以将 `@NeedDictConvert` 添加在类上：

```java
@NeedDictConvert
@RestController
@RequestMapping("/email-template")
public class EmailTemplateController {
}
```

类级注解对该 Controller 中的全部接口生效。

## 6. 兼容模式：`@ConvertDict`

已有 DTO 如果已经定义了文本字段，可以继续使用 `@ConvertDict`：

```java
public class EmailTemplateVO {

    private String keyword1;

    @ConvertDict(
            sourceField = "keyword1",
            sourceClass = EmailKeywordEnum.class
    )
    private String keyword1Txt;
}
```

所需 import：

```java
import com.gear.infra.commons.dict.ConvertDict;
```

`@ConvertDict` 必须添加在 `String` 类型的目标文本字段上：

- `sourceField`：同一对象中编码字段的 Java 字段名。
- `sourceClass`：实现 `BaseEnum` 的字典枚举。

接口标记 `@NeedDictConvert` 后，组件会在响应序列化前将描述写入 `keyword1Txt`。

新接口优先使用 `@DictCode`。`@ConvertDict` 主要用于兼容已有 DTO 或确实需要在 Java 对象中读取文本值的场景。

## 7. 多值编码

编码字段支持英文逗号分隔的多值：

```java
@DictCode(sourceClass = EmailKeywordEnum.class)
private String keywords;
```

字段值：

```text
USER_NAME, EMAIL,UNKNOWN
```

响应结果：

```json
{
  "keywords": "USER_NAME, EMAIL,UNKNOWN",
  "keywordsTxt": "用户名称,邮箱地址,UNKNOWN"
}
```

转换时会去除每个编码两侧的空格，输出文本使用英文逗号连接。

## 8. 嵌套对象与集合

字典转换支持以下响应结构：

- 普通 Java 对象
- 父类和子类字段
- 多层统一响应包装
- `Collection`
- `Map` 的 value
- Java 数组

示例：

```java
@NeedDictConvert
@GetMapping("/list")
public Result<List<EmailTemplateVO>> list() {
    return Result.success(emailTemplateService.list());
}
```

无需针对 `Result` 等统一响应类型编写适配器，组件会处理内部 DTO。

## 9. 转换规则

### 9.1 未知编码

编码在枚举中不存在时，文本字段保留原编码：

```json
{
  "keyword1": "UNKNOWN",
  "keyword1Txt": "UNKNOWN"
}
```

### 9.2 空值

编码为 `null` 时：

- `@ConvertDict` 会将实体文本字段设置为 `null`，避免复用 DTO 时保留旧描述。
- `@DictCode` 的虚拟文本字段遵循主工程 Jackson 的 null 输出策略。
- 如果主工程配置为不输出 null，编码字段和虚拟文本字段都不会输出。

### 9.3 同名字段冲突

如果 DTO 已经存在与虚拟文本字段同名的真实 JSON 属性，真实属性优先，组件不会重复输出虚拟字段。

例如已经存在 `statusTxt` 时：

```java
@DictCode(sourceClass = StatusEnum.class)
private String status;

private String statusTxt;
```

响应中的 `statusTxt` 使用真实字段值。

### 9.4 JSON 范围

`@DictCode` 只对 JSON 响应生效，包括：

- `application/json`
- `application/*+json`

文本、XML、文件下载、流式响应等非 JSON 响应不会增加虚拟文本字段。

## 10. 注解对比

| 注解 | 标记位置 | 是否需要声明文本字段 | 主要用途 |
| --- | --- | --- | --- |
| `@NeedDictConvert` | Controller 类或方法 | 不适用 | 开启当前接口的字典转换 |
| `@DictCode` | 编码字段 | 否 | JSON 自动增加同级文本字段，推荐新接口使用 |
| `@ConvertDict` | 文本字段 | 是 | 将文本写回 Java 对象，兼容已有 DTO |

## 11. 常见问题

### 11.1 添加了 `@DictCode`，但没有返回 `Txt` 字段

依次检查：

1. Controller 类或方法是否添加了 `@NeedDictConvert`。
2. 响应 Content-Type 是否为 JSON。
3. 主工程是否使用 Spring Boot 管理的 Jackson `ObjectMapper`。
4. 字段是否会被 Jackson 正常序列化。
5. 主工程是否排除了 `com.gear.infra.commons.dict.autoconfigure.DictJacksonAutoConfiguration`。

### 11.2 `Txt` 字段返回了原始 code

说明编码未匹配到枚举值。检查：

1. 枚举是否实现 `BaseEnum`。
2. `getCode()` 是否返回预期编码。
3. 字段值与枚举编码的字符串形式是否一致。
4. 编码中是否包含额外空格或大小写差异。

### 11.3 主工程自定义了 `ObjectMapper`

建议让 `ObjectMapper` 继续由 Spring Boot 管理，并通过 `Jackson2ObjectMapperBuilderCustomizer` 定制。

如果完全自行创建 `ObjectMapper`，需要手动注册模块：

```java
objectMapper.registerModule(new DictJacksonModule());
```

```java
import com.gear.infra.commons.dict.jackson.DictJacksonModule;
```

### 11.4 可以同时使用 `@DictCode` 和 `@ConvertDict` 吗

技术上可以，但通常没有必要。建议按场景选择：

- 只需要接口 JSON 展示文本：使用 `@DictCode`。
- Java 业务逻辑中也需要读取文本字段：使用 `@ConvertDict`。

## 12. 推荐实践

1. 新增接口默认使用 `@DictCode`，避免为每个编码字段维护对应的 `Txt` 成员。
2. 统一让业务枚举实现 `BaseEnum`，不要在 Controller 中手动编写转换逻辑。
3. 仅在需要字典文本的接口上添加 `@NeedDictConvert`。
4. 文本字段命名优先使用默认的 `<codeField>Txt`，减少前端理解成本。
5. 需要兼容旧响应结构时保留 `@ConvertDict`，不要一次性破坏下游接口。
