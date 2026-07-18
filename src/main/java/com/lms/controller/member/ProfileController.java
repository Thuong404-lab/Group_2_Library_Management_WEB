package com.lms.controller.member;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.User;
import com.lms.entity.Member;
import com.lms.repository.BorrowDetailRepository;
import com.lms.service.MemberFavoriteService;
import com.lms.service.MembershipService;
import com.lms.service.ProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.lms.config.CustomUserDetails;

/**
 * ProfileController - Quản lý Hồ sơ Thành viên
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/member")
public class ProfileController extends LocalizedControllerSupport {

    private static final int PAGE_SIZE = 10;

    private final ProfileService profileService;
    private final MemberFavoriteService memberFavoriteService;
    private final MembershipService membershipService;
    private final BorrowDetailRepository borrowDetailRepository;

    // Inject ProfileService xử lý logic cốt lõi giống như bên Thủ thư
    public ProfileController(ProfileService profileService,
                             MemberFavoriteService memberFavoriteService,
                             MembershipService membershipService,
                             BorrowDetailRepository borrowDetailRepository) {
        this.profileService = profileService;
        this.memberFavoriteService = memberFavoriteService;
        this.membershipService = membershipService;
        this.borrowDetailRepository = borrowDetailRepository;
    }

    // UC-4.1: View Profile
    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        if (!model.containsAttribute("member")) {
            User member = profileService.getProfile(username);
            model.addAttribute("member", member);
        }
        Member membership = membershipService.getMemberByUsername(username);
        model.addAttribute("membership", membership);
        model.addAttribute("activeBorrowsCount",
                borrowDetailRepository.countActiveBorrowedBooks(membership.getMemberId()));
        return "member/profile";
    }

    // UC-4.2: Update Profile Information
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam(required = false) String username,
                                @RequestParam(required = false) org.springframework.web.multipart.MultipartFile avatarFile,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        String currentUsername = principal.getName();
        String newUsername = (username != null && !username.trim().isEmpty()) ? username.trim() : currentUsername;
        
        try {
            profileService.updateProfile(currentUsername, newUsername, fullName, email, phone, avatarFile);
            
            // Cập nhật lại thông tin trong phiên đăng nhập (Session/SecurityContext) để thanh điều hướng đồng bộ ngay lập tức
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                User sessionUser = customUserDetails.getUser();
                User updatedUser = profileService.getProfile(newUsername);
                
                sessionUser.setFullName(updatedUser.getFullName());
                sessionUser.setAvatar(updatedUser.getAvatar());
                sessionUser.setEmail(updatedUser.getEmail());
                sessionUser.setPhone(updatedUser.getPhone());
                
                if (!currentUsername.equals(newUsername)) {
                    CustomUserDetails newCustomUserDetails = new CustomUserDetails(
                        sessionUser, newUsername, customUserDetails.getPassword(),
                        customUserDetails.isEnabled() ? "Active" : "Inactive",
                        customUserDetails.getAccountId(), customUserDetails.getAuthorities(),
                        customUserDetails.getAttributes()
                    );
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth = 
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            newCustomUserDetails, authentication.getCredentials(), authentication.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", message("backend.profile.updated"));
            return "redirect:/member/profile?updated";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.profile.updateFailed", e));
            
            // Preserve user input on error
            User tempUser = new User();
            tempUser.setFullName(fullName);
            tempUser.setEmail(email);
            tempUser.setPhone(phone);
            try {
                tempUser.setAvatar(profileService.getProfile(currentUsername).getAvatar());
                tempUser.setStatus(profileService.getProfile(currentUsername).getStatus());
            } catch (Exception ignored) {}
            redirectAttributes.addFlashAttribute("member", tempUser);
            
            return "redirect:/member/profile";
        }
    }

    // UC-4.3: Change Password
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // Kiểm tra khớp mật khẩu gõ lại ngay tại Controller trước khi gọi xuống Service
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", message("backend.password.mismatch"));
            return "redirect:/member/profile";
        }

        try {
            String username = principal.getName();
            profileService.changePassword(username, oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", message("backend.password.changed"));
            return "redirect:/member/profile?passwordChanged";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/member/profile";
    }

    // UC-4.4: View Favorites List
    @GetMapping("/favorites")
    public String viewFavorites(Principal principal,
                                Model model,
                                @RequestParam(defaultValue = "0") int page) {
        if (principal == null) {
            return "redirect:/login";
        }

        model.addAttribute("favorites", memberFavoriteService.getMyFavorites(
                principal.getName(), PageRequest.of(Math.max(0, page), PAGE_SIZE)));

        return "member/favorites";
    }

    @GetMapping("/reviews")
    public String redirectToReviews() {
        return "redirect:/member/interaction/reviews";
    }
}
