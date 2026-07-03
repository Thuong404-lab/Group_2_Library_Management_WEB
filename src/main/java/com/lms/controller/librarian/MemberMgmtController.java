package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.MembershipTier;
import com.lms.entity.Notification;
import com.lms.entity.Role;
import com.lms.entity.Transaction;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.enums.UserStatus;

import org.springframework.stereotype.Controller;

import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Controller
@RequestMapping("/librarian")
public class MemberMgmtController {

    private final AccountRepository accountRepository;

    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public MemberMgmtController(AccountRepository accountRepository,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            NotificationRepository notificationRepository,
            MemberNotificationRepository memberNotificationRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    // UC-18.1: Member Accounts Management
    @GetMapping("/members")
    public String viewMemberList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("accountId").ascending());
        Page<Account> accounts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            accounts = accountRepository.searchMemberAccounts(keyword.trim(), pageRequest);
        } else {
            accounts = accountRepository.findMemberAccounts(pageRequest);
        }

        Map<Integer, Member> memberByUserId = new HashMap<>();

        for (Account account : accounts.getContent()) {
            if (account.getUser() != null && account.getUser().getId() != null) {
                memberRepository.findByUserId(account.getUser().getId())
                        .ifPresent(member -> memberByUserId.put(account.getUser().getId(), member));
            }
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("memberByUserId", memberByUserId);
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        addCurrentUser(model, userDetails);

        return "librarian/member-list";
    }

    @GetMapping("/members/create")
    public String showCreateMemberForm(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        addCurrentUser(model, userDetails);

        return "librarian/create-member";
    }

    @PostMapping("/members/create")
    @Transactional
    public String createMemberAccount(@RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false, defaultValue = "") String status,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String trimmedFullName = fullName.trim();
        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();

        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trimmedFullName);
        formValues.put("email", trimmedEmail);
        formValues.put("phone", trimmedPhone);
        formValues.put("username", trimmedUsername);
        formValues.put("tierId", tierId);
        formValues.put("status", status);
        model.addAttribute("formValues", formValues);

        if (trimmedFullName.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "fullName", "Họ tên không được để trống.");
        }

        if (trimmedEmail.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "email", "Email không được để trống.");
        }

        if (!trimmedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return createMemberFormWithError(model, userDetails, "email", "Email không đúng định dạng.");
        }

        if (trimmedUsername.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "username", "Username không được để trống.");
        }

        if (password.isBlank()) {
            return createMemberFormWithError(model, userDetails, "password", "Mật khẩu không được để trống.");
        }

        if (accountRepository.existsByUsername(trimmedUsername)) {
            return createMemberFormWithError(model, userDetails, "username", "Username đã tồn tại.");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            return createMemberFormWithError(model, userDetails, "email", "Email đã được sử dụng.");
        }

        if (trimmedPhone.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại không được để trống.");
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại phải gồm đúng 10 chữ số.");
        }

        if (userRepository.existsByPhone(trimmedPhone)) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại đã được sử dụng.");
        }

        if (tierId == null) {
            return createMemberFormWithError(model, userDetails, "tierId",
                    "Vui lòng chọn hạng thành viên.");
        }

        if (status.isBlank()) {
            return createMemberFormWithError(model, userDetails, "status",
                    "Vui lòng chọn trạng thái thành viên.");
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            return createMemberFormWithError(model, userDetails, "tierId",
                    "Hạng thành viên không hợp lệ.");
        }

        Role memberRole = roleRepository.findByNameIgnoreCase("MEMBER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role MEMBER trong database."));

        User user = new User();
        user.setFullName(trimmedFullName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setUsername(trimmedUsername);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(status);
        account.getRoles().add(memberRole);
        accountRepository.save(account);

        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/edit/{id}")
    @Transactional
    public String updateMemberAccount(@PathVariable Integer id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam Integer tierId,
            @RequestParam(defaultValue = "Active") String status,
            RedirectAttributes redirectAttributes) {

        Account account = accountRepository.findById(id).orElse(null);

        if (account == null || account.getUser() == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();

        if (accountRepository.existsByUsernameAndAccountIdNot(trimmedUsername, account.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByEmailAndIdNot(trimmedEmail, account.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng.");
            return "redirect:/librarian/members";
        }

        if (trimmedPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không được để trống.");
            return "redirect:/librarian/members";
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại phải gồm đúng 10 chữ số.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByPhoneAndIdNot(trimmedPhone, account.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại đã được sử dụng.");
            return "redirect:/librarian/members";
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            redirectAttributes.addFlashAttribute("error", "Hạng thành viên không hợp lệ.");
            return "redirect:/librarian/members";
        }

        User user = account.getUser();
        user.setFullName(fullName.trim());
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));

        account.setUsername(trimmedUsername);
        account.setStatus(status);

        Member member = memberRepository.findByUserId(user.getId()).orElse(new Member());
        member.setUser(user);
        member.setTier(tier);

        userRepository.save(user);
        accountRepository.save(account);
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(@PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        account.setStatus("Inactive");

        if (account.getUser() != null) {
            account.getUser().setStatus(UserStatus.Inactive);
        }

        accountRepository.save(account);

        redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    // UC-14.2: Manage Fines & Violations
    @GetMapping("/members/fines")
    public String manageFines(Model model) {
        // TODO: Implement - Hiển thị trang quản lý phạt
        return "librarian/fines";
    }

    @PostMapping("/members/fines/create")
    public String createFine(@RequestParam Integer memberId,
            @RequestParam Double amount,
            @RequestParam String reason,
            Model model) {
        // TODO: Implement - Tạo khoản phạt mới cho Member
        // TODO: Cập nhật Wallet
        // TODO: Tạo Transaction
        // TODO: Gửi Notification cho Member
        return "redirect:/librarian/members/fines?created";
    }

    // UC-14.3: View Transaction History
    @GetMapping("/members/transactions")
    public String viewAllTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            Model model) {
        int currentPage = Math.max(page, 0);
        int pageSize = 10;
        var pageable = org.springframework.data.domain.PageRequest.of(currentPage, pageSize);

        org.springframework.data.domain.Page<Transaction> transactionPage;
        if (type == null || type.trim().isEmpty()) {
            transactionPage = transactionRepository
                    .findAllByOrderByTransactionDateDesc(pageable);
        } else {
            transactionPage = transactionRepository
                    .findAllByTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                            type.trim(), pageable);
        }

        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("selectedType", type);

        return "librarian/transaction_history";
    }

    // UC-14.4: Top Up Member Account
    @GetMapping("/members/topup")
    public String showTopupDesk(@RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();

        if (!trimmedKeyword.isEmpty()) {
            Optional<Member> selectedMember = findMemberForTopup(trimmedKeyword);

            if (selectedMember.isPresent()) {
                Member member = selectedMember.get();
                Wallet wallet = getOrCreateWallet(member);
                model.addAttribute("selectedMember", member);
                model.addAttribute("walletBalance", wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance());
            } else {
                model.addAttribute("error", "Không tìm thấy thành viên phù hợp với thông tin tra cứu.");
            }
        }

        model.addAttribute("activeMenu", "topup");
        model.addAttribute("keyword", trimmedKeyword);
        model.addAttribute(
                "recentTopups",
                transactionRepository.findTop5ByTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc("TOP_UP")
        );
        addCurrentUser(model, userDetails);

        return "librarian/topup-desk";
    }

    @PostMapping("/members/topup")
    @Transactional
    public String topUpMemberAccount(@RequestParam Integer memberId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false, defaultValue = "") String note,
            RedirectAttributes redirectAttributes) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Số tiền nạp phải lớn hơn 0.");
            return "redirect:/librarian/members/topup";
        }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thành viên cần nạp tiền.");
            return "redirect:/librarian/members/topup";
        }

        Wallet wallet = getOrCreateWallet(member);
        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        wallet.setBalance(currentBalance.add(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType("TOP_UP");
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);

        sendTopupNotification(member, amount, wallet.getBalance(), paymentMethod, note);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã nạp " + formatCurrency(amount) + " đ cho " + getMemberDisplayName(member) + "."
        );
        return "redirect:/librarian/members/topup?keyword=" + member.getMemberId();
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
    }

    private String createMemberFormWithError(Model model, CustomUserDetails userDetails,
            String fieldName, String message) {
        model.addAttribute("fieldErrors", Map.of(fieldName, message));
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));
        addCurrentUser(model, userDetails);
        return "librarian/create-member";
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception e) {
            return UserStatus.Active;
        }
    }

    private Optional<Member> findMemberForTopup(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Optional.empty();
        }

        String value = keyword.trim();
        String memberIdText = value.toUpperCase(Locale.ROOT).startsWith("MEM-")
                ? value.substring(4)
                : value;

        if (memberIdText.matches("\\d+")) {
            Optional<Member> byId = memberRepository.findById(Integer.parseInt(memberIdText));
            if (byId.isPresent()) {
                return byId;
            }
        }

        return memberRepository.findByUserEmail(value)
                .or(() -> memberRepository.findByUserPhone(value))
                .or(() -> memberRepository.findByAccountUsername(value));
    }

    private Wallet getOrCreateWallet(Member member) {
        return walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setMember(member);
                    wallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    private void sendTopupNotification(Member member,
            BigDecimal amount,
            BigDecimal newBalance,
            String paymentMethod,
            String note) {
        Notification notification = new Notification();
        notification.setTitle("Nạp tiền vào ví thành công");
        notification.setContent(
                "Bạn đã nạp tiền " + formatCurrency(amount) + " đ vào ví. "
                        + "Số dư hiện tại: " + formatCurrency(newBalance) + " đ. "
                        + "Phương thức: " + formatPaymentMethod(paymentMethod)
                        + (note == null || note.trim().isEmpty() ? "" : ". Ghi chú: " + note.trim())
        );
        notification.setCreatedDate(LocalDateTime.now());
        notification.setStatus("Active");
        Notification savedNotification = notificationRepository.save(notification);

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(member.getMemberId(), savedNotification.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(savedNotification);
        memberNotification.setIsRead(false);
        memberNotificationRepository.save(memberNotification);
    }

    private String getMemberDisplayName(Member member) {
        if (member.getUser() == null || member.getUser().getFullName() == null || member.getUser().getFullName().isBlank()) {
            return "thành viên #" + member.getMemberId();
        }

        return member.getUser().getFullName();
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String formatPaymentMethod(String paymentMethod) {
        if ("TRANSFER".equalsIgnoreCase(paymentMethod)) {
            return "Chuyển khoản";
        }

        return "Tiền mặt";
    }
}
