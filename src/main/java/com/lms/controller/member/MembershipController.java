package com.lms.controller.member;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import thêm các class cần thiết
import com.lms.entity.MembershipTier;
import com.lms.service.MembershipService;
// =================================================================

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * MembershipController - Quản lý Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/member/membership")
public class MembershipController {

    // =================================================================
    // Huỳnh Gia Hưng bổ sung đoạn này: Khai báo Service và tạo Constructor Injection
    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
    // =================================================================

    // UC-5.1: View Benefits & Privileges
    @GetMapping("/benefits")
    public String viewBenefits(Principal principal, Model model) {
        // TODO: Implement - Lấy MembershipTier của Member hiện tại
        // TODO: Hiển thị quyền lợi theo Tier (Bronze, Silver, Gold, Platinum)

        // =================================================================
        // Huỳnh Gia Hưng bổ sung đoạn này: Gọi service lấy dữ liệu và nạp vào Model
        if (principal != null) {
            // Do logic cơ bản, tạm thời truyền một id giả lập (ví dụ: id = 1)
            // Bạn có thể đổi số 1 này thành cách lấy ID thực tế từ hệ thống Security của nhóm bạn (nếu có)
            Integer currentMemberId = 1;

            MembershipTier tierBenefits = membershipService.getBenefits(currentMemberId);

            // Đẩy dữ liệu sang file HTML template (View)
            model.addAttribute("tierBenefits", tierBenefits);
        }
        // =================================================================

        return "member/benefits";
    }

    // UC-5.2: View Membership Tier
    @GetMapping("/tier")
    public String viewMembershipTier(Principal principal, Model model) {
        // TODO: Implement - Hiển thị Tier hiện tại + điểm tích lũy
        // TODO: Hiển thị tiến trình lên hạng tiếp theo
        return "member/membership-tier";
    }
}