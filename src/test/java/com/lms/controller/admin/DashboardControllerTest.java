package com.lms.controller.admin;

import com.lms.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardControllerTest {

    @Test
    void legacyLogsRouteRedirectsToFullyPopulatedSystemLogsRoute() {
        DashboardController controller = new DashboardController(mock(AdminDashboardService.class));

        assertThat(controller.viewSystemLogs(3, new ExtendedModelMap()))
                .isEqualTo("redirect:/admin/system/logs?page=3");
    }

    @Test
    void legacyLogsRouteNormalizesNegativePage() {
        DashboardController controller = new DashboardController(mock(AdminDashboardService.class));

        assertThat(controller.viewSystemLogs(-2, new ExtendedModelMap()))
                .isEqualTo("redirect:/admin/system/logs?page=0");
    }
}
