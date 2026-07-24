package com.lms.dto.request;

import com.lms.util.AcquisitionRequestPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberBookAcquisitionRequest {

    @NotBlank(message = "{validation.acquisition.titleRequired}")
    @Size(min = AcquisitionRequestPolicy.TITLE_MIN_LENGTH,
            max = AcquisitionRequestPolicy.TITLE_MAX_LENGTH, message = "{validation.acquisition.titleRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.titleLetters}")
    private String title;
    @NotBlank(message = "{validation.acquisition.authorRequired}")
    @Size(min = AcquisitionRequestPolicy.AUTHOR_MIN_LENGTH,
            max = AcquisitionRequestPolicy.AUTHOR_MAX_LENGTH, message = "{validation.acquisition.authorRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.authorLetters}")
    private String author;

    @Size(max = AcquisitionRequestPolicy.ISBN_INPUT_MAX_LENGTH, message = "{validation.acquisition.isbnMaximum}")
    private String isbn;

    @Size(max = AcquisitionRequestPolicy.PUBLISHER_MAX_LENGTH,
            message = "{backend.acquisition.publisherMaximum}")
    @Pattern(regexp = "^\\s*$|(?s).*\\p{L}.*", message = "{backend.acquisition.publisherLetters}")
    private String publisher;

    @Min(value = AcquisitionRequestPolicy.MIN_PUBLICATION_YEAR, message = "{validation.acquisition.year}")
    private Integer publicationYear;

    @NotBlank(message = "{validation.acquisition.reasonRequired}")
    @Size(min = AcquisitionRequestPolicy.REASON_MIN_LENGTH,
            max = AcquisitionRequestPolicy.REASON_MAX_LENGTH, message = "{validation.acquisition.reasonRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.acquisition.reasonLetters}")
    private String requestReason;

    @Size(max = AcquisitionRequestPolicy.REFERENCE_URL_MAX_LENGTH,
            message = "{backend.acquisition.referenceMaximum}")
    @Pattern(regexp = "(?i)^(?:\\s*|https?://.+)$", message = "{validation.httpUrl}")
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
