package com.lms.service;

import com.lms.entity.Book;
import jakarta.servlet.http.HttpSession;
import java.util.List;

public interface CartService {
    boolean addToCart(HttpSession session, Integer bookId);
    void removeFromCart(HttpSession session, Integer bookId);
    List<Book> getCartItems(HttpSession session);
    int getCartCount(HttpSession session);
    boolean isInCart(HttpSession session, Integer bookId);
    void clearCart(HttpSession session);
}
