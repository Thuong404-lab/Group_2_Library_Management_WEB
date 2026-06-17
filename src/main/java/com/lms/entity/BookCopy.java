package com.lms.entity;

import com.lms.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BookCopies")
@Getter
@Setter
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
}
