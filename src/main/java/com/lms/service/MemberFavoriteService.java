package com.lms.service;

import com.lms.entity.Favorites;

import java.util.List;
import java.util.Set;

public interface MemberFavoriteService {

    void addToFavorites(String username, Integer bookId);

    void removeFromFavorites(String username, Integer bookId);

    List<Favorites> getMyFavorites(String username);

    Set<Integer> getMyFavoriteBookIds(String username);
}
