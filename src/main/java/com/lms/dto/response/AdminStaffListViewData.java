package com.lms.dto.response;

import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import org.springframework.data.domain.Page;

import java.util.Map;

public record AdminStaffListViewData(
        Page<Staff> staffPage,
        Map<Integer, StaffAccount> accountByUserId) {
}
