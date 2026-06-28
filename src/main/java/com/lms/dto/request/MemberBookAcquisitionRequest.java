package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberBookAcquisitionRequest {

    @NotBlank(message = "Tên sách không được để trống")
    @Size(max = 255, message = "Tên sách không được vượt quá 255 ký tự")
    private String title;
    private String author;

    public MemberBookAcquisitionRequest() {
    }

    public MemberBookAcquisitionRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}