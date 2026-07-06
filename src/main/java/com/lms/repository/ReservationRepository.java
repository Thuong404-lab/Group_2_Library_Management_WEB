package com.lms.repository;
import com.lms.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    long countByStatusIgnoreCase(String status);

    List<Reservation> findByMemberMemberIdOrderByReservationDateDesc(Integer memberId);

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
