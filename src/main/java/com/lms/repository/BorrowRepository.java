package com.lms.repository;

import com.lms.entity.Borrow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Integer> {

    // --- CÁC PHƯƠNG THỨC GỐC CỦA NHÓM BẠN (GIỮ NGUYÊN HOÀN TOÀN) ---
    long countByStatusIgnoreCase(String status);

    List<Borrow> findTop5ByOrderByBorrowDateDesc();

    long countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("select month(b.borrowDate), year(b.borrowDate), count(b) " +
            "from Borrow b " +
            "where b.borrowDate >= :startDate and b.borrowDate < :endDate " +
            "group by year(b.borrowDate), month(b.borrowDate) " +
            "order by year(b.borrowDate), month(b.borrowDate)")
    List<Object[]> countMonthlyBorrows(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Borrow> findByMember_MemberIdOrderByBorrowDateDesc(Integer memberId);


    // --- BỔ SUNG ĐẦY ĐỦ CÁC PHƯƠNG THỨC PHÂN TRANG (PAGINATION) THEO CHUẨN JPA ---

    // 1. Phân trang lọc theo trạng thái (Status)
    Page<Borrow> findByStatus(String status, Pageable pageable);

    // 1b. Lấy toàn bộ theo trạng thái (không phân trang) - dùng cho scheduled job
    List<Borrow> findAllByStatus(String status);

    // 2. Phân trang lọc theo từ khóa tìm kiếm (Keyword: fullName, email, phone hoặc borrowId)
    @Query("SELECT b FROM Borrow b WHERE " +
            "LOWER(b.member.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.member.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.member.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(b.borrowId AS string) LIKE CONCAT('%', :keyword, '%')")
    Page<Borrow> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 3. Phân trang lọc kết hợp cả Trạng thái (Status) và Từ khóa (Keyword)
    @Query("SELECT b FROM Borrow b WHERE b.status = :status AND (" +
            "LOWER(b.member.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.member.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.member.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(b.borrowId AS string) LIKE CONCAT('%', :keyword, '%'))")
    Page<Borrow> findByStatusAndKeyword(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    List<Borrow> findByMember_User_Phone(String phone);

    @Query("SELECT MIN(b.borrowDate) FROM Borrow b WHERE b.member.memberId = :memberId")
    java.time.LocalDateTime findMinBorrowDateByMemberId(@Param("memberId") Integer memberId);
}
