package com.lms.controller.admin;

import com.lms.entity.Account;
import com.lms.enums.UserStatus;
import com.lms.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * AccountController - Quản lý tài khoản thành viên
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * // UC-18.1: Member Accounts Management
     * 
     * @GetMapping
     *             public String listAccounts(@RequestParam(defaultValue = "0") int
     *             page,
     * @RequestParam(required = false) String keyword,
     *                        Model model) {
     *                        PageRequest pageRequest = PageRequest.of(page, 10,
     *                        Sort.by("accountId").descending());
     * 
     *                        Page<Account> accounts;
     * 
     *                        if (keyword != null && !keyword.trim().isEmpty()) {
     *                        accounts =
     *                        accountRepository.searchAccounts(keyword.trim(),
     *                        pageRequest);
     *                        } else {
     *                        accounts = accountRepository.findAll(pageRequest);
     *                        }
     * 
     *                        model.addAttribute("accounts", accounts);
     *                        model.addAttribute("keyword", keyword);
     * 
     *                        return "admin/accounts";
     *                        }
     */

    @GetMapping
    public String listAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("accountId").descending());

        Page<Account> accounts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            accounts = accountRepository.searchAccounts(keyword.trim(), pageRequest);
        } else {
            accounts = accountRepository.findAll(pageRequest);
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);

        return "admin/accounts";
    }

    @GetMapping("/search")
    public String searchAccounts(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listAccounts(page, keyword, model);
    }

    @PostMapping("/status/{id}")
    public String changeAccountStatus(@PathVariable Integer id,
            @RequestParam String status) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            return "redirect:/admin/accounts?notFound";
        }

        account.setStatus(status);

        if (account.getUser() != null) {
            try {
                account.getUser().setStatus(UserStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                // Nếu status không khớp enum UserStatus thì chỉ cập nhật Account.status
            }
        }

        accountRepository.save(account);

        return "redirect:/admin/accounts?statusChanged";
    }

    @PostMapping("/reset-password/{id}")
    public String resetPassword(@PathVariable Integer id) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            return "redirect:/admin/accounts?notFound";
        }

        account.setPasswordHash(passwordEncoder.encode("Test@1234"));
        accountRepository.save(account);

        return "redirect:/admin/accounts?passwordReset";
    }

    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            return "redirect:/admin/accounts?notFound";
        }

        // Soft delete: không xóa khỏi DB, chỉ khóa tài khoản
        account.setStatus("Inactive");

        if (account.getUser() != null) {
            account.getUser().setStatus(UserStatus.Inactive);
        }

        accountRepository.save(account);

        return "redirect:/admin/accounts?deleted";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        return "admin/create-account";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            return "redirect:/admin/accounts?notFound";
        }

        model.addAttribute("account", account);
        return "admin/edit-account";
    }
}
