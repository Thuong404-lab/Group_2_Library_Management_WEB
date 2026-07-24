package com.lms.repository;

import com.lms.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    long countByStatusIgnoreCase(String status);
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r JOIN MemberAccount ma ON r.member = ma.member WHERE ma.username = :username ORDER BY r.reservationDate DESC")
    java.util.List<Reservation> findReservationsByUsername(@org.springframework.data.repository.query.Param("username") String username);

    List<Reservation> findByMemberMemberIdOrderByReservationDateDesc(Integer memberId);

    List<Reservation> findByMember_MemberIdOrderByReservationDateDesc(Integer memberId);

    List<Reservation> findByMemberMemberIdAndStatusInOrderByReservationDateDesc(
            Integer memberId,
            Collection<String> statuses);

    List<Reservation> findByStatusInOrderByReservationDateAsc(Collection<String> statuses);

    List<Reservation> findByBook_BookIdAndStatusInOrderByReservationDateAsc(
            Integer bookId,
            Collection<String> statuses);

    List<Reservation> findByStatusIgnoreCaseAndReservationDateLessThanEqual(
            String status, java.time.LocalDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.reservationId = :reservationId")
    Optional<Reservation> findByIdForUpdate(@Param("reservationId") Integer reservationId);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.book.bookId = :bookId
              and r.member.memberId <> :memberId
              and upper(r.status) in :statuses
            """)
    boolean existsActiveReservationByOtherMemberForBook(
            @Param("bookId") Integer bookId,
            @Param("memberId") Integer memberId,
            @Param("statuses") List<String> statuses);

    @Query("""
            select count(r)
            from Reservation r
            where r.book.bookId = :bookId
              and r.member.memberId <> :memberId
              and upper(trim(r.status)) in :statuses
            """)
    long countActiveReservationsByOtherMemberForBook(
            @Param("bookId") Integer bookId,
            @Param("memberId") Integer memberId,
            @Param("statuses") List<String> statuses);
    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.member.memberId = :memberId
              and r.book.bookId = :bookId
              and upper(r.status) in :statuses
            """)
    boolean existsActiveReservationForMemberAndBook(
            @Param("memberId") Integer memberId,
            @Param("bookId") Integer bookId,
            @Param("statuses") List<String> statuses);

    @Query("""
            select count(r)
            from Reservation r
            where upper(r.status) in :statuses
            """)
    long countByNormalizedStatuses(@Param("statuses") List<String> statuses);
}
