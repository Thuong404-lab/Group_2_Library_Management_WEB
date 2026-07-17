package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.repository.BookRepository;
import com.lms.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CartServiceImpl implements CartService {

    private final BookRepository bookRepository;
    private static final String CART_SESSION_KEY = "BOOK_CART";

    public CartServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> getOrCreateCart(HttpSession session) {
        Set<Integer> cart = (Set<Integer>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new HashSet<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    @Override
    public void addToCart(HttpSession session, Integer bookId) {
        Set<Integer> cart = getOrCreateCart(session);
        cart.add(bookId);
    }

    @Override
    public void removeFromCart(HttpSession session, Integer bookId) {
        Set<Integer> cart = getOrCreateCart(session);
        cart.remove(bookId);
    }

    @Override
    public List<Book> getCartItems(HttpSession session) {
        Set<Integer> cart = getOrCreateCart(session);
        if (cart.isEmpty()) return new ArrayList<>();
        return bookRepository.findAllById(cart);
    }

    @Override
    public int getCartCount(HttpSession session) {
        return getOrCreateCart(session).size();
    }

    @Override
    public void clearCart(HttpSession session) {
        session.setAttribute(CART_SESSION_KEY, new HashSet<Integer>());
    }
}