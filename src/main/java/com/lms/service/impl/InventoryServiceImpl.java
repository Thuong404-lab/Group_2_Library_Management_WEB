package com.lms.service.impl;

import com.lms.entity.Author;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Category;
import com.lms.entity.Genre;
import com.lms.entity.Shelf;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.AuthorRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.ShelfRepository;
import com.lms.service.InventoryService;
import com.lms.service.LocalizedMessageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class InventoryServiceImpl implements InventoryService {
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_BORROWED = "Borrowed";
    private static final String STATUS_WAITING_PICKUP = "Waiting_Pickup";
    private static final String STATUS_UNAVAILABLE = "Unavailable";
    private static final String STATUS_ACTIVE = "Active";
    private static final Set<String> ALLOWED_BOOK_CONDITIONS = Set.of(
            "New",
            "Minor damage",
            "Severely damaged",
            "Lost book");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ShelfRepository shelfRepository;
    private final AuthorRepository authorRepository;
    private final com.lms.repository.ReservationRepository reservationRepository;
    private final com.lms.repository.NotificationRepository notificationRepository;
    private final com.lms.repository.MemberNotificationRepository memberNotificationRepository;
    private final com.lms.service.EmailService emailService;
    private final LocalizedMessageService messages;

    public InventoryServiceImpl(BookRepository bookRepository,
            CategoryRepository categoryRepository,
            GenreRepository genreRepository,
            BookItemRepository bookItemRepository,
            BorrowDetailRepository borrowDetailRepository,
            ShelfRepository shelfRepository,
            AuthorRepository authorRepository,
            com.lms.repository.ReservationRepository reservationRepository,
            com.lms.repository.NotificationRepository notificationRepository,
            com.lms.repository.MemberNotificationRepository memberNotificationRepository,
            com.lms.service.EmailService emailService,
            LocalizedMessageService messages) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.shelfRepository = shelfRepository;
        this.authorRepository = authorRepository;
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.emailService = emailService;
        this.messages = messages;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    @Override
    public long countBooks() {
        return bookRepository.count();
    }

    @Override
    public long countCategories() {
        return categoryRepository.count();
    }

    @Override
    public long countGenres() {
        return genreRepository.count();
    }

    @Override
    public Map<String, Long> getInventoryStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put(STATUS_AVAILABLE, bookItemRepository.countByStatusIgnoreCase(STATUS_AVAILABLE));
        counts.put(STATUS_BORROWED, bookItemRepository.countByStatusIgnoreCase(STATUS_BORROWED));
        counts.put(STATUS_WAITING_PICKUP, bookItemRepository.countByStatusIgnoreCase(STATUS_WAITING_PICKUP));
        counts.put(STATUS_UNAVAILABLE, bookItemRepository.countByStatusIgnoreCase(STATUS_UNAVAILABLE));
        return counts;
    }

    @Override
    public Map<String, Long> performInventoryAudit() {
        return getInventoryStatusCounts();
    }

    @Override
    public Book findBookById(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", bookId)));
    }

    @Override
    public void addNewBook(String title, String isbn, Integer genreId, Integer quantity, String description,
            String coverImageUrl, Integer shelfId, String bookCondition, String authorName) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.titleRequired"));
        }
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.isbnRequired"));
        }
        if (authorName == null || authorName.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.authorRequired"));
        }
        if (quantity == null || quantity < 0 || quantity > 100) {
            throw new ValidationException(messages.get("librarian.inventory.validation.quantity"));
        }
        if (shelfId == null) {
            throw new ValidationException(messages.get("backend.inventory.shelfRequired"));
        }
        if (coverImageUrl == null || coverImageUrl.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.imageRequired"));
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ValidationException(messages.get("backend.inventory.validGenreRequired")));

        Book book = new Book();
        book.setTitle(title.trim());
        book.setIsbn(isbn.trim());
        book.setGenre(genre);
        book.setStatus(STATUS_ACTIVE);
        if (description != null && !description.trim().isEmpty()) {
            book.setDescription(description.trim());
        }
        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
            book.setCoverImageUrl(coverImageUrl.trim());
        }

        // Handle author
        if (authorName != null && !authorName.trim().isEmpty()) {
            String finalAuthorName = authorName.trim();
            Author author = authorRepository.findByAuthorNameIgnoreCase(finalAuthorName)
                    .orElseGet(() -> {
                        Author newAuthor = new Author();
                        newAuthor.setAuthorName(finalAuthorName);
                        return authorRepository.save(newAuthor);
                    });
            Set<Author> authors = new HashSet<>();
            authors.add(author);
            book.setAuthors(authors);
        }

        book = bookRepository.save(book);

        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ValidationException(messages.get("backend.inventory.shelfNotFound")));

        int copies = quantity;

        for (int i = 1; i <= copies; i++) {
            BookItem item = new BookItem();
            item.setBook(book);
            item.setShelf(shelf);
            item.setBarcode(
                    String.format(
                            "BC%03d-%03d",
                            book.getBookId(),
                            i));
            if (bookCondition != null && !bookCondition.trim().isEmpty()) {
                item.setBookCondition(bookCondition.trim());
            }
            item.setStatus(conditionRank(item.getBookCondition()) >= 3
                    ? STATUS_UNAVAILABLE
                    : STATUS_AVAILABLE);

            bookItemRepository.save(item);
        }
    }

    @Override
    public void updateBook(Integer bookId, String title, String isbn, Integer genreId, String status,
            String coverImageUrl, Integer shelfId, String description, String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.authorRequired"));
        }
        if (shelfId == null) {
            throw new ValidationException(messages.get("backend.inventory.shelfRequired"));
        }
        Book book = findBookById(bookId);
        if (title != null && !title.trim().isEmpty()) {
            book.setTitle(title.trim());
        }
        if (isbn != null && !isbn.trim().isEmpty()) {
            book.setIsbn(isbn.trim());
        }
        if (genreId != null) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new ValidationException(messages.get("backend.inventory.validGenreRequired")));
            book.setGenre(genre);
        }
        if (status != null && !status.trim().isEmpty()) {
            String normalizedStatus = status.trim();
            if (!STATUS_ACTIVE.equalsIgnoreCase(normalizedStatus)
                    && !"Inactive".equalsIgnoreCase(normalizedStatus)) {
                throw new ValidationException(messages.get("backend.inventory.statusRequired"));
            }
            book.setStatus(normalizedStatus);
        }
        // Chỉ cập nhật ảnh nếu có ảnh mới được upload
        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
            book.setCoverImageUrl(coverImageUrl.trim());
        }
        if (description != null && !description.trim().isEmpty()) {
            book.setDescription(description.trim());
        }

        // Handle author
        if (authorName != null && !authorName.trim().isEmpty()) {
            String finalAuthorName = authorName.trim();
            Author author = authorRepository.findByAuthorNameIgnoreCase(finalAuthorName)
                    .orElseGet(() -> {
                        Author newAuthor = new Author();
                        newAuthor.setAuthorName(finalAuthorName);
                        return authorRepository.save(newAuthor);
                    });
            Set<Author> authors = new HashSet<>();
            authors.add(author);
            book.setAuthors(authors);
        }

        bookRepository.save(book);

        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ValidationException(messages.get("backend.inventory.shelfNotFound")));
        List<BookItem> items = bookItemRepository.findByBook_BookId(bookId);
        for (BookItem item : items) {
            item.setShelf(shelf);
            bookItemRepository.save(item);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void addBookCopies(Integer bookId, Integer quantity, Integer shelfId, String bookCondition) {
        if (quantity == null || quantity < 1 || quantity > 100) {
            throw new ValidationException(messages.get("backend.inventory.copyQuantityInvalid"));
        }
        if (shelfId == null) {
            throw new ValidationException(messages.get("backend.inventory.shelfRequired"));
        }
        String normalizedCondition = bookCondition == null ? "" : bookCondition.trim();
        if (!ALLOWED_BOOK_CONDITIONS.contains(normalizedCondition)) {
            throw new ValidationException(messages.get("backend.inventory.bookConditionInvalid"));
        }

        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", bookId)));
        long currentQuantity = bookItemRepository.countByBook_BookId(bookId);
        if (currentQuantity + quantity > 100) {
            throw new ValidationException(messages.get("backend.inventory.copyQuantityLimit", currentQuantity));
        }
        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ValidationException(messages.get("backend.inventory.shelfNotFound")));
        Set<String> existingBarcodes = bookItemRepository.findByBook_BookId(bookId).stream()
                .map(BookItem::getBarcode)
                .collect(java.util.stream.Collectors.toSet());
        int sequence = 1;

        for (int added = 0; added < quantity; added++) {
            String barcode;
            do {
                barcode = String.format("BC%03d-%03d", bookId, sequence++);
            } while (existingBarcodes.contains(barcode));

            BookItem item = new BookItem();
            item.setBook(book);
            item.setShelf(shelf);
            item.setBarcode(barcode);
            item.setBookCondition(normalizedCondition);
            item.setStatus(conditionRank(normalizedCondition) >= 3
                    ? STATUS_UNAVAILABLE
                    : STATUS_AVAILABLE);
            bookItemRepository.save(item);
            autoAssignNewCopyIfReservationWaiting(item);
            existingBarcodes.add(barcode);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteBookCopies(Integer bookId, List<Integer> bookItemIds) {
        if (bookItemIds == null || bookItemIds.isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.selectCopiesToDelete"));
        }

        List<BookItem> items = bookItemRepository.findAllById(bookItemIds);
        if (items.size() != new HashSet<>(bookItemIds).size()
                || items.stream().anyMatch(item -> !item.getBook().getBookId().equals(bookId))) {
            throw new ValidationException(messages.get("backend.inventory.invalidCopySelection"));
        }

        for (BookItem item : items) {
            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(messages.get("backend.inventory.deleteUnavailableCopyConflict"));
            }
            if (borrowDetailRepository.existsByBookItem_BookItemId(item.getBookItemId())) {
                throw new ConflictException(messages.get("backend.inventory.deleteCopyHistoryConflict"));
            }
        }
        bookItemRepository.deleteAll(items);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateBookCopyCondition(Integer bookId, Integer bookItemId, String bookCondition) {
        String normalizedCondition = bookCondition == null ? "" : bookCondition.trim();
        if (!ALLOWED_BOOK_CONDITIONS.contains(normalizedCondition)) {
            throw new ValidationException(messages.get("backend.inventory.bookConditionInvalid"));
        }

        BookItem item = bookItemRepository.findById(bookItemId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.copyNotFound")));
        if (!item.getBook().getBookId().equals(bookId)) {
            throw new ValidationException(messages.get("backend.inventory.invalidCopySelection"));
        }
        if (STATUS_BORROWED.equalsIgnoreCase(item.getStatus())
                || STATUS_WAITING_PICKUP.equalsIgnoreCase(item.getStatus())) {
            throw new ConflictException(messages.get("backend.inventory.copyConditionLocked"));
        }
        if (conditionRank(normalizedCondition) < conditionRank(item.getBookCondition())) {
            throw new ConflictException(messages.get("backend.inventory.copyConditionCannotImprove"));
        }

        item.setBookCondition(normalizedCondition);
        int rank = conditionRank(normalizedCondition);
        item.setStatus(rank >= 3 ? STATUS_UNAVAILABLE : STATUS_AVAILABLE);
        bookItemRepository.save(item);
    }

    private int conditionRank(String condition) {
        String value = condition == null ? "" : condition.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.contains("lost") || value.contains("mất sách")) return 3;
        if (value.contains("severely") || value.contains("hư hỏng nặng")) return 2;
        if (value.contains("minor") || value.contains("hư hỏng nhẹ")) return 1;
        return 0;
    }

    @Override
    public void updateBookStatus(Integer bookId, String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.statusRequired"));
        }
        String normalizedStatus = status.trim();
        if (!STATUS_ACTIVE.equalsIgnoreCase(normalizedStatus)
                && !"Inactive".equalsIgnoreCase(normalizedStatus)) {
            throw new ValidationException(messages.get("backend.inventory.statusRequired"));
        }
        Book book = findBookById(bookId);
        book.setStatus(normalizedStatus);
        bookRepository.save(book);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void removeBook(Integer bookId) {
        Book book = findBookById(bookId);
        List<BookItem> items = bookItemRepository.findByBook_BookId(bookId);
        for (BookItem item : items) {
            if (STATUS_BORROWED.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(messages.get("backend.inventory.deleteBorrowedConflict"));
            }
        }
        try {
            // Xóa tất cả BookItems
            bookItemRepository.deleteAll(items);
            // Xóa sách
            bookRepository.delete(book);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    messages.get("backend.inventory.deleteHistoryConflict"), ex);
        }
    }

    @Override
    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.categoryNameRequired"));
        }
        if (name.trim().length() > 20) {
            throw new ValidationException(messages.get("backend.inventory.validation.maxLength20"));
        }
        Category category = new Category();
        category.setCategoryName(name.trim());
        categoryRepository.save(category);
    }

    @Override
    public void addGenre(Integer categoryId, String name) {
        if (categoryId == null) {
            throw new ValidationException(messages.get("backend.inventory.categoryRequired"));
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.genreNameRequired"));
        }
        if (name.trim().length() > 20) {
            throw new ValidationException(messages.get("backend.inventory.validation.maxLength20"));
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.categoryNotFound")));

        Genre genre = new Genre();
        genre.setCategory(category);
        genre.setGenreName(name.trim());
        genreRepository.save(genre);
    }

    @Override
    public void updateCategory(Integer categoryId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.categoryNameRequired"));
        }
        if (newName.trim().length() > 20) {
            throw new ValidationException(messages.get("backend.inventory.validation.maxLength20"));
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.categoryNotFound")));
        category.setCategoryName(newName.trim());
        categoryRepository.save(category);
    }

    @Override
    public void updateGenre(Integer genreId, String newName, Integer newCategoryId) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.inventory.genreNameRequired"));
        }
        if (newName.trim().length() > 20) {
            throw new ValidationException(messages.get("backend.inventory.validation.maxLength20"));
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.genreNotFound")));

        if (newCategoryId != null) {
            Category category = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.categoryNotFound")));
            genre.setCategory(category);
        }

        genre.setGenreName(newName.trim());
        genreRepository.save(genre);
    }

    @Override
    public void deleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.genreNotFound")));

        if (bookRepository.existsByGenre_GenreId(genreId)) {
            throw new ConflictException(messages.get("backend.inventory.deleteGenreConflict"));
        }

        genreRepository.delete(genre);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.categoryNotFound")));
        
        if (genreRepository.existsByCategory_CategoryId(categoryId)) {
            throw new ConflictException(messages.get("backend.inventory.deleteCategoryConflict"));
        }
        categoryRepository.delete(category);
    }

    private void autoAssignNewCopyIfReservationWaiting(BookItem item) {
        if (item == null || item.getBook() == null || !STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
            return;
        }
        Integer bookId = item.getBook().getBookId();
        List<com.lms.entity.Reservation> waitingList = reservationRepository.findByBook_BookIdAndStatusInOrderByReservationDateAsc(
                bookId, List.of("Deposit_Paid", "Pending"));
        if (!waitingList.isEmpty()) {
            com.lms.entity.Reservation nextReservation = waitingList.get(0);
            nextReservation.setStatus("Ready");
            reservationRepository.save(nextReservation);

            item.setStatus(STATUS_WAITING_PICKUP);
            bookItemRepository.save(item);

            sendInternalNotification(nextReservation.getMember(),
                    com.lms.enums.NotificationType.RESERVATION, com.lms.enums.NotificationEventType.RESERVATION_APPROVED, com.lms.enums.NotificationSource.SYSTEM,
                    "systemNotification.reservation.ready.title",
                    "systemNotification.reservation.ready.content",
                    nextReservation.getBook() != null ? nextReservation.getBook().getTitle() : "");
        }
    }

    private void sendInternalNotification(com.lms.entity.Member member,
            com.lms.enums.NotificationType type,
            com.lms.enums.NotificationEventType eventType,
            com.lms.enums.NotificationSource source,
            String titleKey,
            String contentKey,
            Object... arguments) {
        if (member == null) {
            return;
        }
        com.lms.entity.Notification notif = new com.lms.entity.Notification();
        messages.prepareNotification(notif, titleKey, contentKey, arguments);
        notif.setNotificationType(type);
        notif.setEventType(eventType);
        notif.setNotificationSource(source);
        notif.setCreatedDate(java.time.LocalDateTime.now());
        notif.setStatus(STATUS_ACTIVE);
        com.lms.entity.Notification saved = notificationRepository.save(notif);

        com.lms.entity.MemberNotification mn = new com.lms.entity.MemberNotification();
        mn.setId(new com.lms.entity.MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        mn.setMember(member);
        mn.setNotification(saved);
        mn.setIsRead(false);
        memberNotificationRepository.save(mn);

        if (emailService != null) {
            String recipientEmail = (member.getUser() != null) ? member.getUser().getEmail() : null;
            String recipientName = (member.getUser() != null) ? member.getUser().getFullName() : "Độc giả";
            if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                final String to = recipientEmail.trim();
                final String name = recipientName;
                final String title = notif.getTitle();
                final String content = notif.getContent();
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendNotificationEmail(to, name, title, content);
                    } catch (Exception ignored) {
                        // Fallback
                    }
                });
            }
        }
    }
}
