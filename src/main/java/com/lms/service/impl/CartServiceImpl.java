package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.repository.BookRepository;
import com.lms.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        Object storedCart = session.getAttribute(CART_SESSION_KEY);
        Set<Integer> cart = storedCart instanceof Set<?>
                ? new LinkedHashSet<>((Set<Integer>) storedCart)
                : new LinkedHashSet<>();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    @Override
    public boolean addToCart(HttpSession session, Integer bookId) {
        Set<Integer> cart = getOrCreateCart(session);
        return cart.add(bookId);
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
        Map<Integer, Book> booksById = new LinkedHashMap<>();
        bookRepository.findAllById(cart).forEach(book -> booksById.put(book.getBookId(), book));
        cart.removeIf(bookId -> !booksById.containsKey(bookId));
        return cart.stream().map(booksById::get).toList();
    }

    @Override
    public int getCartCount(HttpSession session) {
        return getOrCreateCart(session).size();
    }

    @Override
    public boolean isInCart(HttpSession session, Integer bookId) {
        return bookId != null && getOrCreateCart(session).contains(bookId);
    }

    @Override
    public void clearCart(HttpSession session) {
        session.setAttribute(CART_SESSION_KEY, new LinkedHashSet<Integer>());
    }
}
