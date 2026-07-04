package com.lms.repository;

import com.lms.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    long countByStatusIgnoreCase(String status);

    // BỔ SUNG: Lấy danh sách đặt trước của 1 độc giả sắp xếp theo ngày đặt mới nhất
    List<Reservation> findByMember_MemberIdOrderByReservationDateDesc(Integer memberId);
}