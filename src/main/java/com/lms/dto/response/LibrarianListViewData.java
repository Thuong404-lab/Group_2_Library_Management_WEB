package com.lms.dto.response;

import com.lms.entity.StaffAccount;
import com.lms.entity.Staff;
import org.springframework.data.domain.Page;

import java.util.Map;

public record LibrarianListViewData(
        Page<Staff> staffPage,
        Map<Integer, StaffAccount> accountByUserId) {
}
