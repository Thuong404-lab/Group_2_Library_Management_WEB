package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.AccountRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.FavoritesRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.MemberFavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberFavoriteServiceImpl implements MemberFavoriteService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final FavoritesRepository favoritesRepository;

    public MemberFavoriteServiceImpl(AccountRepository accountRepository,
                                     MemberRepository memberRepository,
                                     BookRepository bookRepository,
                                     FavoritesRepository favoritesRepository) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.favoritesRepository = favoritesRepository;
    }

    @Override
    @Transactional
    public void addToFavorites(String username, Integer bookId) {
        System.out.println(">>> ĐÃ VÀO SERVICE: username=" + username + ", bookId=" + bookId);
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách"));

        System.out.println("DEBUG: MemberID = " + member.getMemberId());
        System.out.println("DEBUG: BookID = " + book.getBookId());

        FavoritesId id = new FavoritesId(member.getMemberId(), book.getBookId());

        boolean exists = favoritesRepository.existsById(id);
        System.out.println("DEBUG: Exists in DB? " + exists);

        if (exists) {
            throw new ValidationException("Sách này đã có trong danh sách yêu thích.");
        }

        Favorites favorites = new Favorites();
        favorites.setId(id);
        favorites.setMember(member);
        favorites.setBook(book);

        // Sử dụng save thay vì saveAndFlush nếu dùng @Transactional
        // Hibernate sẽ tự động flush khi transaction kết thúc (commit)
        favoritesRepository.saveAndFlush(favorites);
        System.out.println("DEBUG: Saved Successfully!");
    }

    @Override
    @Transactional
    public void removeFromFavorites(String username, Integer bookId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username));

        FavoritesId id = new FavoritesId(member.getMemberId(), bookId);

        Favorites favorites = favoritesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách yêu thích."));

        favoritesRepository.delete(favorites);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorites> getMyFavorites(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        Member member = memberRepository.findByUserId(account.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả với tài khoản: " + username));

        return favoritesRepository.findByMember_MemberId(member.getMemberId());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> getMyFavoriteBookIds(String username) {
        return getMyFavorites(username).stream()
                .map(favorite -> favorite.getBook().getBookId())
                .collect(Collectors.toSet());
    }
}
