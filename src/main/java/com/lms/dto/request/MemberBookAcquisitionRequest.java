package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberBookAcquisitionRequest {

    @NotBlank(message = "{validation.acquisition.titleRequired}")
    @Size(min = 2, max = 255, message = "{validation.acquisition.titleRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.titleLetters}")
    private String title;
    @NotBlank(message = "{validation.acquisition.authorRequired}")
    @Size(min = 2, max = 255, message = "{validation.acquisition.authorRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.authorLetters}")
    private String author;

    @Size(max = 20, message = "{validation.acquisition.isbnMaximum}")
    private String isbn;

    @Size(max = 255, message = "{backend.acquisition.publisherMaximum}")
    @Pattern(regexp = "^\\s*$|(?s).*\\p{L}.*", message = "{backend.acquisition.publisherLetters}")
    private String publisher;

    @Min(value = 1000, message = "{validation.acquisition.year}")
    @Max(value = 2100, message = "{validation.acquisition.year}")
    private Integer publicationYear;

    @NotBlank(message = "{validation.acquisition.reasonRequired}")
    @Size(min = 10, max = 1000, message = "{validation.acquisition.reasonRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.reasonLetters}")
    private String requestReason;

    @Size(max = 500, message = "{backend.acquisition.referenceMaximum}")
    @Pattern(regexp = "^\\s*$|https?://.+", message = "{validation.httpUrl}")
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
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getReferenceUrl() { return referenceUrl; }
    public void setReferenceUrl(String referenceUrl) { this.referenceUrl = referenceUrl; }
}
