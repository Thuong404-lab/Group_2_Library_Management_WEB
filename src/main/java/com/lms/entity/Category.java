package com.lms.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "Categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;
    @Column(nullable = false, length = 255)
    private String categoryName;

    public Category() {
    }

    public Category(Integer categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
