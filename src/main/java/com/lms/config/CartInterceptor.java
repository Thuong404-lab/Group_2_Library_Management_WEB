package com.lms.config;

import com.lms.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CartInterceptor implements HandlerInterceptor {

    private final CartService cartService;

    public CartInterceptor(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && request.isUserInRole("MEMBER")) {
            HttpSession session = request.getSession();
            int count = cartService.getCartCount(session);
            modelAndView.addObject("cartCount", count);
        }
    }
}