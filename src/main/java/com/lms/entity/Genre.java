package com.lms.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "Genres")
public class Genre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer genreId;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, length = 255)
    private String genreName;
    
    public Integer getGenreId() { return genreId; }
    public void setGenreId(Integer genreId) { this.genreId = genreId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }
}
