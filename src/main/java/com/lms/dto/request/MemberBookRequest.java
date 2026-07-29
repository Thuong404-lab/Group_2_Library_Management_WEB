package com.lms.dto.request;

import com.lms.util.BookRequestPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberBookRequest {

    @NotBlank(message = "{validation.bookRequest.titleRequired}")
    @Size(min = BookRequestPolicy.TITLE_MIN_LENGTH,
            max = BookRequestPolicy.TITLE_MAX_LENGTH, message = "{validation.bookRequest.titleRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.bookRequest.titleLetters}")
    private String title;
    @NotBlank(message = "{validation.bookRequest.authorRequired}")
    @Size(min = BookRequestPolicy.AUTHOR_MIN_LENGTH,
            max = BookRequestPolicy.AUTHOR_MAX_LENGTH, message = "{validation.bookRequest.authorRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.bookRequest.authorLetters}")
    private String author;

    @Size(max = BookRequestPolicy.ISBN_INPUT_MAX_LENGTH, message = "{validation.bookRequest.isbnMaximum}")
    private String isbn;

    @Size(max = BookRequestPolicy.PUBLISHER_MAX_LENGTH,
            message = "{backend.bookRequest.publisherMaximum}")
    @Pattern(regexp = "^\\s*$|(?s).*\\p{L}.*", message = "{backend.bookRequest.publisherLetters}")
    private String publisher;

    @Min(value = BookRequestPolicy.MIN_PUBLICATION_YEAR, message = "{validation.bookRequest.year}")
    private Integer publicationYear;

    @NotBlank(message = "{validation.bookRequest.reasonRequired}")
    @Size(min = BookRequestPolicy.REASON_MIN_LENGTH,
            max = BookRequestPolicy.REASON_MAX_LENGTH, message = "{validation.bookRequest.reasonRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{validation.bookRequest.reasonLetters}")
    private String requestReason;

    @Size(max = BookRequestPolicy.REFERENCE_URL_MAX_LENGTH,
            message = "{backend.bookRequest.referenceMaximum}")
    @Pattern(regexp = "(?i)^(?:\\s*|https?://.+)$", message = "{validation.httpUrl}")
    private String referenceUrl;

    public MemberBookRequest() {
    }

    public MemberBookRequest(String title) {
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
