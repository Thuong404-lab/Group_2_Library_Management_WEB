package com.lms.service;

import com.lms.entity.Book;
import jakarta.servlet.http.HttpSession;
import java.util.List;

public interface CartService {
    void addToCart(HttpSession session, Integer bookId);
    void removeFromCart(HttpSession session, Integer bookId);
    List<Book> getCartItems(HttpSession session);
    int getCartCount(HttpSession session);
    void clearCart(HttpSession session);
}