# DownloadHelper 使用指南

`DownloadHelper` 用于在 Servlet/Spring MVC 接口中输出附件下载响应，统一处理下载响应头、UTF-8 文件名、跨域响应头暴露和输入流复制。

类路径模板文件应放在 `src/main/resources/templates/` 目录。

## 引入方式

`DownloadHelper` 是带默认方法的接口。Controller 或其他需要下载能力的组件实现该接口后即可直接调用其方法：

```java
import com.gear.infra.commons.helper.DownloadHelper;

@RestController
public class ReportController implements DownloadHelper {
}
```

## 下载类路径模板

将模板放在 `src/main/resources/templates/import-template.xlsx`，然后使用 `download`：

```java
@GetMapping("/template")
public void downloadTemplate(HttpServletResponse response) throws IOException {
    download(response, "import-template.xlsx");
}
```

该方法会：

- 仅从 `classpath:templates/` 读取文件；
- 自动根据文件名推断 MIME 类型，无法识别时使用 `application/octet-stream`；
- 自动关闭模板输入流；
- 拒绝包含 `/`、`\\` 或 `..` 的文件名，避免越过模板目录读取资源。

如果模板不存在，将抛出 `FileNotFoundException`。

## 下载业务生成的内容

对于数据库、对象存储或运行时生成的文件，使用 `downloadStream`：

```java
@GetMapping("/report")
public void downloadReport(HttpServletResponse response) throws IOException {
    InputStream inputStream = reportService.export();
    try {
        downloadStream(response, "monthly-report.xlsx", inputStream,
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    } finally {
        inputStream.close();
    }
}
```

`downloadStream` 不会关闭传入的输入流，调用方必须负责关闭。若内容类型不重要，可调用三参数重载，它会使用 `application/octet-stream`：

```java
downloadStream(response, "export.dat", inputStream);
```

常用媒体类型示例：

| 文件类型 | `MediaType` |
| --- | --- |
| PDF | `MediaType.APPLICATION_PDF` |
| JSON | `MediaType.APPLICATION_JSON` |
| PNG | `MediaType.IMAGE_PNG` |
| Excel `.xlsx` | `MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")` |

## 仅设置下载响应头

在需要自行写入响应输出流时，可先调用 `prepareAttachmentResponse`：

```java
prepareAttachmentResponse(response, "result.pdf", MediaType.APPLICATION_PDF);
response.getOutputStream().write(pdfBytes);
response.flushBuffer();
```

生成的响应包含：

- `Content-Disposition: attachment`：触发浏览器下载，并使用 UTF-8 编码文件名；
- `Content-Type`：使用传入的媒体类型；
- `Access-Control-Expose-Headers`：保留已有设置，并以不区分大小写的方式加入 `Content-Disposition` 和 `filename`；
- `filename`：URL 编码后的文件名，供前端直接读取。

## 前端读取文件名

跨域调用时，前端可读取自定义 `filename` 响应头并解码：

```javascript
const response = await fetch('/api/report');
const encodedFileName = response.headers.get('filename');
const fileName = encodedFileName ? decodeURIComponent(encodedFileName) : 'download';
const blob = await response.blob();

const url = URL.createObjectURL(blob);
const link = document.createElement('a');
link.href = url;
link.download = fileName;
link.click();
URL.revokeObjectURL(url);
```

## 注意事项

- 下载方法会写入并刷新响应，调用前不要向同一个 `HttpServletResponse` 写入其他内容。
- 文件名不能为空；模板下载的文件名必须是单个文件名，不能带目录。
- `Content-Disposition` 是标准文件名来源；`filename` 是为方便前端使用保留的自定义响应头。
- 在业务接口中应根据实际文件类型传入正确的 `MediaType`，以便浏览器和客户端正确识别内容。
