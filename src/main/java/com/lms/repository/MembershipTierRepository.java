package com.lms.repository;

import com.lms.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {

    /** Lấy tất cả tier, sắp xếp tăng dần theo điều kiện chi tiêu — sort ở DB, không sort ở memory */
    List<MembershipTier> findAllByOrderByConditionAsc();

    Optional<MembershipTier> findFirstByOrderByConditionAscTierIdAsc();

    /** Kiểm tra tên tier đã tồn tại (khi tạo mới) */
    boolean existsByTierNameIgnoreCase(String tierName);

    /** Kiểm tra tên tier đã tồn tại, bỏ qua tier hiện tại (khi chỉnh sửa) */
    boolean existsByTierNameIgnoreCaseAndTierIdNot(String tierName, Integer tierId);

}
