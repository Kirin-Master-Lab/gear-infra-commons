package com.gear.infra.commons.helper;

import com.gear.infra.commons.constant.BaseConstant;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

public interface DownloadHelper {

    /**
     * 准备附件下载响应头。
     *
     * @param response 响应对象
     * @param fileName 下载时展示的文件名
     */
    default void prepareAttachmentResponse(HttpServletResponse response, String fileName) throws IOException {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("fileName must not be null");
        }
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }

        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        String contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

        Set<String> exposedHeaders = new LinkedHashSet<String>();
        String existsExposeHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
        if (existsExposeHeaders != null && !existsExposeHeaders.trim().isEmpty()) {
            String[] existsExposeHeaderArray = existsExposeHeaders.split(",");
            for (String existsExposeHeader : existsExposeHeaderArray) {
                if (existsExposeHeader != null && !existsExposeHeader.trim().isEmpty()) {
                    exposedHeaders.add(existsExposeHeader.trim());
                }
            }
        }
        exposedHeaders.add(HttpHeaders.CONTENT_DISPOSITION);
        exposedHeaders.add(BaseConstant.FILE_NAME_HEADER);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", exposedHeaders));
        response.setHeader(BaseConstant.FILE_NAME_HEADER, encodedFileName);
    }

    /**
     * 将指定输入流作为附件下载。
     *
     * @param response 响应对象
     * @param fileName 下载时展示的文件名
     * @param inputStream 文件输入流，由调用方负责关闭
     */
    default void downloadStream(HttpServletResponse response, String fileName, InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        prepareAttachmentResponse(response, fileName);
        StreamUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 从类路径模板目录下载模板文件。
     *
     * @param response 响应对象
     * @param templateFileName 模板文件名
     */
    default void download(HttpServletResponse response, String templateFileName) throws IOException {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        if (templateFileName == null) {
            throw new IllegalArgumentException("templateFileName must not be null");
        }
        if (templateFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("templateFileName must not be blank");
        }
        if (templateFileName.contains("/") || templateFileName.contains("\\") || templateFileName.contains("..")) {
            throw new IllegalArgumentException("templateFileName must be a file name under templates directory");
        }

        String templatePath = BaseConstant.TEMPLATE_DIR + templateFileName;
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(templatePath);

        if (inputStream == null) {
            throw new FileNotFoundException("/templates下文件未找到: classpath:/" + templatePath);
        }

        try (InputStream templateInputStream = inputStream) {
            downloadStream(response, templateFileName, templateInputStream);
        }
    }
}
