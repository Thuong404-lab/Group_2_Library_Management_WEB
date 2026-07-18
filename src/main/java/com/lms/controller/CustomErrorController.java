package com.lms.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomErrorController extends LocalizedControllerSupport {

    @GetMapping("/403")
    public String accessDenied(HttpServletResponse response, Model model) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        model.addAttribute("error", message("error.forbidden.title"));
        model.addAttribute("errorMessage", message("backend.error.accessDeniedResource"));
        return "error";
    }
}
