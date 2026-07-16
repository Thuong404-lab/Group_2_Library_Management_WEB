package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberBookAcquisitionRequest {

    @NotBlank(message = "Tên sách không được để trống")
    @Size(min = 2, max = 255, message = "Tên sách phải có từ 2 đến 255 ký tự")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "Tên sách không được chỉ gồm số hoặc ký tự đặc biệt")
    private String title;
    @NotBlank(message = "Tác giả không được để trống")
    @Size(min = 2, max = 255, message = "Tên tác giả phải có từ 2 đến 255 ký tự")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "Tên tác giả không được chỉ gồm số hoặc ký tự đặc biệt")
    private String author;

    @Size(max = 255, message = "Nhà xuất bản không được vượt quá 255 ký tự")
    @Pattern(regexp = "^\\s*$|(?s).*\\p{L}.*", message = "Nhà xuất bản không được chỉ gồm số hoặc ký tự đặc biệt")
    private String publisher;

    @Min(value = 1000, message = "Năm xuất bản không hợp lệ")
    @Max(value = 2100, message = "Năm xuất bản không hợp lệ")
    private Integer publicationYear;

    @NotBlank(message = "Lý do đề xuất không được để trống")
    @Size(min = 10, max = 1000, message = "Lý do đề xuất phải có từ 10 đến 1000 ký tự")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "Lý do đề xuất không được chỉ gồm số hoặc ký tự đặc biệt")
    private String requestReason;

    @Size(max = 500, message = "Link tham khảo không được vượt quá 500 ký tự")
    @Pattern(regexp = "^\\s*$|https?://.+", message = "Link tham khảo phải bắt đầu bằng http:// hoặc https://")
    private String referenceUrl;

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
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getReferenceUrl() { return referenceUrl; }
    public void setReferenceUrl(String referenceUrl) { this.referenceUrl = referenceUrl; }
}
