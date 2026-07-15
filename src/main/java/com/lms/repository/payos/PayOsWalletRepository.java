package com.lms.repository.payos;

import com.lms.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayOsWalletRepository extends JpaRepository<Wallet, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.member.memberId = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(@Param("memberId") Integer memberId);
}
