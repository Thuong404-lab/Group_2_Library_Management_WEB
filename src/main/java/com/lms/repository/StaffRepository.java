package com.lms.repository;

import com.lms.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface StaffRepository extends JpaRepository<Staff, Integer> {

        Optional<Staff> findByUserId(Integer userId);
        Page<Staff> findByStaffTypeIgnoreCase(String staffType, Pageable pageable);
        long countByStaffTypeIgnoreCase(String staffType);

        @Query("""
                        SELECT s
                        FROM Staff s
                        WHERE LOWER(s.staffType) = LOWER(:staffType)
                          AND (
                                LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                             OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                             OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          )
                        """)
        Page<Staff> searchByStaffTypeAndKeyword(@Param("staffType") String staffType,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT s
                        FROM Staff s
                        WHERE LOWER(s.staffType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                           OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        """)
        Page<Staff> searchStaffByKeyword(@Param("keyword") String keyword, Pageable pageable);
}