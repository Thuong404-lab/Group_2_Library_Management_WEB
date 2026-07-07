package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.AuditLogService;
import com.lms.service.MemberFavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberFavoriteServiceImpl implements MemberFavoriteService {

    private final MemberAccountRepository memberAccountRepository;
    private final BookRepository bookRepository;
    private final FavoritesRepository favoritesRepository;
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;

    public MemberFavoriteServiceImpl(MemberAccountRepository memberAccountRepository,
                                     BookRepository bookRepository,
                                     FavoritesRepository favoritesRepository,
                                     ReservationRepository reservationRepository,
                                     AuditLogService auditLogService) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookRepository = bookRepository;
        this.favoritesRepository = favoritesRepository;
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
    }

    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả");
        }
        return member;
    }

    @Override
    @Transactional
    public void addToFavorites(String username, Integer bookId) {
        Member member = getMemberByUsername(username);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách"));

        FavoritesId id = new FavoritesId(member.getMemberId(), book.getBookId());
        if (favoritesRepository.existsById(id)) {
            throw new ValidationException("Sách này đã có trong danh sách yêu thích.");
        }

        Favorites favorites = new Favorites();
        favorites.setId(id);
        favorites.setMember(member);
        favorites.setBook(book);

        favoritesRepository.saveAndFlush(favorites);
    }

    @Override
    @Transactional
    public void removeFromFavorites(String username, Integer bookId) {
        Member member = getMemberByUsername(username);

        FavoritesId id = new FavoritesId(member.getMemberId(), bookId);
        Favorites favorites = favoritesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách yêu thích."));

        favoritesRepository.delete(favorites);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorites> getMyFavorites(String username) {
        Member member = getMemberByUsername(username);

        return favoritesRepository.findByMember_MemberId(member.getMemberId());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> getMyFavoriteBookIds(String username) {
        return getMyFavorites(username).stream()
                .map(favorite -> favorite.getBook().getBookId())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveBook(String username, Integer bookId) throws Exception {
        Member member = getMemberByUsername(username);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách không tồn tại"));

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(java.time.LocalDateTime.now());
        reservation.setStatus("Pending");

        reservationRepository.save(reservation);

        auditLogService.log(
                ActionType.RESERVE_BOOK,
                "Member " + username + " dat giu cho sach #" + book.getBookId() + " - " + book.getTitle() + ".");
    }

    // TRIỂN KHAI BỔ SUNG: Trích xuất danh sách sách để hiển thị Đề Cử trên Dashboard
    @Override
    @Transactional(readOnly = true)
    public List<Book> getFavoriteBooksByMember(String username) {
        return getMyFavorites(username).stream()
                .map(Favorites::getBook)
                .collect(Collectors.toList());
    }
}
