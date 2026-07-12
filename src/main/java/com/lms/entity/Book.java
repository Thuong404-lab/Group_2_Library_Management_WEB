package com.lms.entity;
import jakarta.persistence.*;
import java.util.Set;
@Entity
@Table(name = "Books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookId;
    @ManyToOne
    @JoinColumn(name = "genre_id")
    private Genre genre;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(unique = true, length = 20)
    private String isbn;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    @Column(length = 500)
    private String coverImageUrl;
    @Column(length = 50)
    private String status = "Active";


    @ManyToMany
    @JoinTable(
        name = "`BookAuthors`",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors;

    public Book() {
    }

    public Book(Integer bookId, Genre genre, String title, String isbn, String description, String coverImageUrl, String status, Set<Author> authors) {
        this.bookId = bookId;
        this.genre = genre;
        this.title = title;
        this.isbn = isbn;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.status = status;
        this.authors = authors;
    }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Set<Author> getAuthors() { return authors; }
    public void setAuthors(Set<Author> authors) { this.authors = authors; }
}
