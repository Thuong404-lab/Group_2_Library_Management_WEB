package com.lms.entity;

import com.lms.enums.BorrowStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class BorrowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberEmail; // Lưu email member để xác định ai mượn
    private LocalDate borrowDate;
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private BorrowStatus status; // PENDING, APPROVED, RETURNED, REJECTED

    @ElementCollection
    private List<String> bookBarcodes; // Danh sách mã vạch sách mượn

    private String notes;

    // Getters và Setters...
}