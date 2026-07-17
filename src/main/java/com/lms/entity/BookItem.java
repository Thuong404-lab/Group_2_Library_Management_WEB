package com.lms.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "BookItems")
public class BookItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookItemId;
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    @ManyToOne
    @JoinColumn(name = "shelf_id")
    private Shelf shelf;
    @Column(unique = true, nullable = false, length = 50)
    private String barcode;
    @Column(length = 50)
    private String status = "Available";

    @Column(name = "book_condition", columnDefinition = "NVARCHAR(50)")
    private String bookCondition = "Mới";

    @Column(name = "damage_note", columnDefinition = "NVARCHAR(255)")
    private String damageNote;

    public BookItem() {
    }

    public BookItem(Integer bookItemId, Book book, Shelf shelf, String barcode, String status, String bookCondition) {
        this.bookItemId = bookItemId;
        this.book = book;
        this.shelf = shelf;
        this.barcode = barcode;
        this.status = status;
        this.bookCondition = bookCondition;
    }

    public Integer getBookItemId() { return bookItemId; }
    public void setBookItemId(Integer bookItemId) { this.bookItemId = bookItemId; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public Shelf getShelf() { return shelf; }
    public void setShelf(Shelf shelf) { this.shelf = shelf; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookCondition() { return bookCondition; }
    public void setBookCondition(String bookCondition) { this.bookCondition = bookCondition; }
    public String getDamageNote() { return damageNote; }
    public void setDamageNote(String damageNote) { this.damageNote = damageNote; }
}