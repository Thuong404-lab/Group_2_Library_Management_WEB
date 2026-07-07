package com.lms.repository;

import com.lms.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    Optional<SystemSetting> findBySettingKeyIgnoreCase(String settingKey);

    boolean existsBySettingKeyIgnoreCase(String settingKey);
}
