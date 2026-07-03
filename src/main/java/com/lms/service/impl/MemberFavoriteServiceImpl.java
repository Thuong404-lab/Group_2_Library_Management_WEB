package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.MemberFavoriteService;
import com.lms.service.MemberNotificationService; // Import service mới tiêm
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
    private final ReservationRepository reservationRepository;
    private final MemberNotificationService notificationService; // Thêm biến thành viên

    public MemberFavoriteServiceImpl(AccountRepository accountRepository,
                                     MemberRepository memberRepository,
                                     BookRepository bookRepository,
                                     FavoritesRepository favoritesRepository,
                                     ReservationRepository reservationRepository,
                                     MemberNotificationService notificationService) { // Inject qua Constructor
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.favoritesRepository = favoritesRepository;
        this.reservationRepository = reservationRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void addToFavorites(String username, Integer bookId) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả"));

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
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả"));

        FavoritesId id = new FavoritesId(member.getMemberId(), bookId);
        Favorites favorites = favoritesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách yêu thích."));

        favoritesRepository.delete(favorites);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorites> getMyFavorites(String username) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả"));

        return favoritesRepository.findByMember_MemberId(member.getMemberId());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> getMyFavoriteBookIds(String username) {
        return getMyFavorites(username).stream()
                .map(favorite -> favorite.getBook().getBookId())
                .collect(Collectors.toSet());
    }

    // ======= CẢI TIẾN: KHI MEMBER GỬI ĐẶT TRƯỚC SẼ BÁO LIBRARIAN CỦA HỆ THỐNG =======
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveBook(String username, Integer bookId) throws Exception {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách không tồn tại"));

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(java.time.LocalDateTime.now());
        reservation.setStatus("Pending");

        reservationRepository.save(reservation);

        // Phát ra tín hiệu thông báo thời gian thực lưu trữ đến nhóm quản trị Thủ thư
        String memberName = (member.getUser() != null) ? member.getUser().getFullName() : username;
        notificationService.sendNotificationToAllLibrarians(
                "Yêu cầu chuẩn bị sách đặt trước",
                "Độc giả " + memberName + " vừa tạo một đơn đặt trước trực tuyến cuốn sách: '" + book.getTitle() + "'."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getFavoriteBooksByMember(String username) {
        return getMyFavorites(username).stream()
                .map(Favorites::getBook)
                .collect(Collectors.toList());
    }
}