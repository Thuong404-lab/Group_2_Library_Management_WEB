package com.lms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lms.entity.Favorites;
import com.lms.entity.Book;
import java.util.List;
import java.util.Set;

public interface MemberFavoriteService {
    void addToFavorites(String username, Integer bookId);
    void removeFromFavorites(String username, Integer bookId);
    List<Favorites> getMyFavorites(String username);
    Page<Favorites> getMyFavorites(String username, Pageable pageable);
    Set<Integer> getMyFavoriteBookIds(String username);
    Set<Integer> getAvailableFavoriteBookIds(String username);

    List<Book> getFavoriteBooksByMember(String username);
}
