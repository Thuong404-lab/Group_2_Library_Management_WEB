package com.lms.repository;

import com.lms.entity.Favorites;
import com.lms.entity.FavoritesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoritesRepository extends JpaRepository<Favorites, FavoritesId> {

    List<Favorites> findByMember_MemberId(Integer memberId);
}