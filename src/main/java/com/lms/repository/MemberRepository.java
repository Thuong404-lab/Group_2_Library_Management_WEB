package com.lms.repository;

import com.lms.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {

    // Hàm tìm kiếm theo Email người dùng
    Optional<Member> findByUserEmail(String email);

    // Dùng @Query để đảm bảo an toàn tuyệt đối, không lo sai quy tắc đặt tên hàm của Spring Data
    @Query("SELECT m FROM Member m WHERE m.user.id = :userId")
    Optional<Member> findByUserId(@Param("userId") Integer userId);
}