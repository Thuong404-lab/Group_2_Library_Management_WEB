package com.lms.service;

import com.lms.dto.response.LibrarianListViewData;

import java.util.Map;

public interface LibrarianDashboardService {

    Map<String, Object> getDashboardData();

    Map<String, Object> getDashboardData(int reviewPage, int requestPage);

    Map<String, Object> getDashboardData(int bookPage, int reviewPage, int requestPage);

    Map<String, Object> getDashboardData(int bookPage, int reviewPage, int requestPage, String keyword);

    Map<String, Object> getDashboardData(int bookPage, int shelfPage, int reviewPage, int requestPage, String keyword);

    Map<String, Object> getDashboardData(int bookPage, int shelfPage, int reviewPage, int requestPage,
                                         String keyword, String bookCondition);

    Map<String, Object> getBookManagementData(int bookPage, int shelfPage, String keyword,
                                              String bookCondition, String subsection, String tab);

    Map<String, Object> getStatisticsData();

    LibrarianListViewData getLibrarianList(int page, String keyword, String status);
}
