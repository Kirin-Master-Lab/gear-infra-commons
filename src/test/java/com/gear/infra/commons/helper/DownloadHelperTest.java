package com.gear.infra.commons.helper;

import com.gear.infra.commons.constant.BaseConstant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadHelperTest {

    private final DownloadHelper downloadHelper = new DownloadHelper() {
    };

    @Test
    void prepareAttachmentResponseUsesUtf8ContentDisposition() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        downloadHelper.prepareAttachmentResponse(response, "配置 模板.xlsx");

        String contentDisposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        String fileNameHeader = response.getHeader(BaseConstant.FILE_NAME_HEADER);

        assertAll(
                () -> assertNotNull(contentDisposition),
                () -> assertTrue(contentDisposition.contains("attachment")),
                () -> assertTrue(contentDisposition.contains("filename*=")),
                () -> assertTrue(contentDisposition.contains("UTF-8''")),
                () -> assertFalse(contentDisposition.contains("+")),
                () -> assertEquals("配置 模板.xlsx", URLDecoder.decode(fileNameHeader, StandardCharsets.UTF_8.name())),
                () -> assertFalse(fileNameHeader.contains("+"))
        );
    }

    @Test
    void prepareAttachmentResponseMergesExposeHeaders() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Trace-Id, filename");

        downloadHelper.prepareAttachmentResponse(response, "demo.xlsx");

        String exposeHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);

        assertAll(
                () -> assertTrue(exposeHeaders.contains("X-Trace-Id")),
                () -> assertTrue(exposeHeaders.contains(HttpHeaders.CONTENT_DISPOSITION)),
                () -> assertTrue(exposeHeaders.contains(BaseConstant.FILE_NAME_HEADER)),
                () -> assertEquals(1, countHeader(exposeHeaders, BaseConstant.FILE_NAME_HEADER))
        );
    }

    @Test
    void downloadStreamCopiesContentAndDoesNotCloseInputStream() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        CloseAwareInputStream inputStream = new CloseAwareInputStream("hello".getBytes(StandardCharsets.UTF_8));

        downloadHelper.downloadStream(response, "demo.txt", inputStream);

        assertAll(
                () -> assertEquals("hello", response.getContentAsString()),
                () -> assertFalse(inputStream.isClosed())
        );
    }

    @Test
    void methodsRejectInvalidArguments() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.prepareAttachmentResponse(null, "demo.xlsx")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.prepareAttachmentResponse(response, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.prepareAttachmentResponse(response, " ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.downloadStream(response, "demo.xlsx", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(null, "demo.xlsx")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(response, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(response, " ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(response, "../demo.xlsx")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(response, "dir/demo.xlsx")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> downloadHelper.download(response, "dir\\demo.xlsx"))
        );
    }

    @Test
    void downloadThrowsFileNotFoundWhenTemplateMissing() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(FileNotFoundException.class, () -> downloadHelper.download(response, "missing-template.xlsx"));
    }

    private static int countHeader(String exposeHeaders, String headerName) {
        int count = 0;
        String[] headers = exposeHeaders.split(",");
        for (String header : headers) {
            if (headerName.equals(header.trim())) {
                count++;
            }
        }
        return count;
    }

    private static class CloseAwareInputStream extends ByteArrayInputStream {

        private boolean closed;

        CloseAwareInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }
}
