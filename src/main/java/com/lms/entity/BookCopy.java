package com.lms.entity;

import com.lms.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BookCopies")

public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "copy_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private StorageLocation storage;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "condition_pct")
    private Integer conditionPct = 100;

    public BookCopy(Book book, Integer id, String barcode, StorageLocation storage, CopyStatus status, Integer conditionPct) {
        this.book = book;
        this.id = id;
        this.barcode = barcode;
        this.storage = storage;
        this.status = status;
        this.conditionPct = conditionPct;
    }

    public BookCopy() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public StorageLocation getStorage() {
        return storage;
    }

    public void setStorage(StorageLocation storage) {
        this.storage = storage;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    public Integer getConditionPct() {
        return conditionPct;
    }

    public void setConditionPct(Integer conditionPct) {
        this.conditionPct = conditionPct;
    }
}
