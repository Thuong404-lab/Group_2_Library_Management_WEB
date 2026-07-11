package com.lms.service;

import com.lms.dto.response.LibrarianListViewData;

import java.util.Map;

public interface LibrarianDashboardService {

    Map<String, Object> getDashboardData(int bookPage);

    LibrarianListViewData getLibrarianList(int page, String keyword);
}
