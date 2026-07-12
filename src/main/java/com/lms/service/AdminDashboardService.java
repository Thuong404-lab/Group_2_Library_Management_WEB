package com.lms.service;

import com.lms.dto.response.AdminStaffListViewData;

import java.util.Map;

public interface AdminDashboardService {

    Map<String, Object> getDashboardData();

    AdminStaffListViewData getStaffList(int page, String keyword);
}
