package com.lms.dto.response;

public class ReportExport {
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public ReportExport(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
