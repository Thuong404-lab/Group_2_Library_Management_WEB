package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.repository.BookRepository;
import com.lms.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private List<Integer> getOrCreateCart(HttpSession session) {
        Object storedCart = session.getAttribute(CART_SESSION_KEY);
        List<Integer> cart = storedCart instanceof List<?>
                ? (List<Integer>) storedCart
                : storedCart instanceof Set<?>
                        ? new ArrayList<>((Set<Integer>) storedCart)
                        : new ArrayList<>();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    @Override
    public boolean addToCart(HttpSession session, Integer bookId) {
        List<Integer> cart = getOrCreateCart(session);
        cart.add(bookId);
        return true;
    }

    @Override
    public void removeFromCart(HttpSession session, Integer bookId) {
        List<Integer> cart = getOrCreateCart(session);
        cart.removeIf(id -> id.equals(bookId));
    }

    @Override
    public List<Book> getCartItems(HttpSession session) {
        List<Integer> cart = getOrCreateCart(session);
        if (cart.isEmpty())
            return new ArrayList<>();
        Set<Integer> uniqueIds = new LinkedHashSet<>(cart);
        return bookRepository.findAllById(uniqueIds);
    }

    @Override
    public int getCartCount(HttpSession session) {
        return getOrCreateCart(session).size();
    }

    @Override
    public void clearCart(HttpSession session) {
        session.setAttribute(CART_SESSION_KEY, new ArrayList<Integer>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getQuantityInCart(HttpSession session, Integer bookId) {
        List<Integer> cart = (List<Integer>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null || cart.isEmpty()) {
            return 0;
        }
        return (int) cart.stream().filter(id -> id.equals(bookId)).count();
    }

    @Override
    public boolean isInCart(HttpSession session, Integer bookId) {
        return bookId != null && getOrCreateCart(session).contains(bookId);
    }
}
