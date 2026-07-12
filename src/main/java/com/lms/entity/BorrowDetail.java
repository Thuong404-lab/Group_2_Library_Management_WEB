package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "`BorrowDetails`")
public class BorrowDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer borrowDetailId;
    @ManyToOne
    @JoinColumn(name = "borrow_id", nullable = false)
    private Borrow borrow;
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    @ManyToOne
    @JoinColumn(name = "book_item_id")
    private BookItem bookItem;
    @Column(nullable = false)
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Integer renewCount = 0;
    @Column(length = 50)
    private String status = "Borrowed";
    @Column(name = "condition_note", length = 255)
    private String conditionNote;

    public BorrowDetail() {
    }

    public BorrowDetail(Integer borrowDetailId, Borrow borrow, Book book, BookItem bookItem, LocalDateTime dueDate, LocalDateTime returnDate, Integer renewCount, String status) {
        this.borrowDetailId = borrowDetailId;
        this.borrow = borrow;
        this.book = book;
        this.bookItem = bookItem;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.renewCount = renewCount;
        this.status = status;
    }

    public Integer getBorrowDetailId() { return borrowDetailId; }
    public void setBorrowDetailId(Integer borrowDetailId) { this.borrowDetailId = borrowDetailId; }
    public Borrow getBorrow() { return borrow; }
    public void setBorrow(Borrow borrow) { this.borrow = borrow; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public BookItem getBookItem() { return bookItem; }
    public void setBookItem(BookItem bookItem) { this.bookItem = bookItem; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public Integer getRenewCount() { return renewCount; }
    public void setRenewCount(Integer renewCount) { this.renewCount = renewCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String conditionNote) { this.conditionNote = conditionNote; }
}
