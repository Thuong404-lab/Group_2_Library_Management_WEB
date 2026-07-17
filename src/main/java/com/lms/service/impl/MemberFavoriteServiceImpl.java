package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.AuditLogService;
import com.lms.service.FinancialService;
import com.lms.service.MemberFavoriteService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberFavoriteServiceImpl implements MemberFavoriteService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final BookRepository bookRepository;
    private final FavoritesRepository favoritesRepository;
    private final ReservationRepository reservationRepository;
    private final FinancialService financialService;
    private final AuditLogService auditLogService;

    public MemberFavoriteServiceImpl(MemberAccountRepository memberAccountRepository,
                                     BookRepository bookRepository,
                                     FavoritesRepository favoritesRepository,
                                     ReservationRepository reservationRepository,
                                     FinancialService financialService,
                                     AuditLogService auditLogService) {
        this.memberAccountRepository = memberAccountRepository;
        this.bookRepository = bookRepository;
        this.favoritesRepository = favoritesRepository;
        this.reservationRepository = reservationRepository;
        this.financialService = financialService;
        this.auditLogService = auditLogService;
    }
    //lấy ra thông tin cá nhân của một Độc giả (Member) dựa trên tên đăng nhập (username) của người đó.
    private Member getMemberByUsername(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username)));
        Member member = account.getMember();
        if (member == null) {
            throw new ResourceNotFoundException(messages.get("backend.member.currentNotFound"));
        }
        return member;
    }

    @Override
    @Transactional
    public void addToFavorites(String username, Integer bookId) {
        Member member = getMemberByUsername(username);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", bookId)));

        FavoritesId id = new FavoritesId(member.getMemberId(), book.getBookId());
        if (favoritesRepository.existsById(id)) {
            throw new ConflictException(messages.get("backend.favorite.alreadyAdded"));
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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.favorite.notFound")));

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
    public Page<Favorites> getMyFavorites(String username, Pageable pageable) {
        Member member = getMemberByUsername(username);

        return favoritesRepository.findByMember_MemberId(member.getMemberId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> getMyFavoriteBookIds(String username) {
        Member member = getMemberByUsername(username);
        return favoritesRepository.findByMember_MemberId(member.getMemberId()).stream()
                .map(favorite -> favorite.getBook().getBookId())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation reserveBook(String username, Integer bookId) {
        Member member = getMemberByUsername(username);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", bookId)));

        if (reservationRepository.existsActiveReservationForMemberAndBook(
                member.getMemberId(), book.getBookId(), List.of("PENDING", "DEPOSIT_PAID", "READY"))) {
            throw new ConflictException(messages.get("backend.favorite.activeReservationExists"));
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(java.time.LocalDateTime.now());
        reservation.setStatus("Pending");

        reservation = reservationRepository.saveAndFlush(reservation);
        financialService.payReservationDeposit(member.getMemberId(), reservation.getReservationId());
        auditLogService.log(
                ActionType.RESERVE_BOOK,
                messages.get("backend.borrow.audit.reservationRequested", username, book.getBookId(), book.getTitle()));

        return reservation;
    }

    // TRIỂN KHAI BỔ SUNG: Trích xuất danh sách sách để hiển thị Đề Cử trên Dashboard
    @Override
    @Transactional(readOnly = true)
    public List<Book> getFavoriteBooksByMember(String username) {
        Member member = getMemberByUsername(username);
        return favoritesRepository.findByMember_MemberId(member.getMemberId()).stream()
                .map(Favorites::getBook)
                .toList();
    }
}
