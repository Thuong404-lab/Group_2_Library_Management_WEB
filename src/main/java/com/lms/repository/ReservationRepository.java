package com.lms.repository;
import com.lms.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    long countByStatusIgnoreCase(String status);
    
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r JOIN MemberAccount ma ON r.member = ma.member WHERE ma.username = :username ORDER BY r.reservationDate DESC")
    java.util.List<Reservation> findReservationsByUsername(@org.springframework.data.repository.query.Param("username") String username);
}
