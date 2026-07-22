package com.lms.dto.response;

import org.springframework.data.domain.Page;

import java.util.Map;

public record LibrarianListViewData(
        Page<LibrarianListItemResponse> staffPage,
        Map<String, Long> summaryCounts,
        String keyword,
        String selectedStatus) {
}
