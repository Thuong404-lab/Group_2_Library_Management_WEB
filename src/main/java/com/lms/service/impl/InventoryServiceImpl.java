package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Category;
import com.lms.entity.Genre;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BookDisposalRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.ShelfRepository;
import com.lms.entity.Shelf;
import com.lms.service.InventoryService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class InventoryServiceImpl implements InventoryService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final BookItemRepository bookItemRepository;
    private final BookDisposalRepository bookDisposalRepository;
    private final ShelfRepository shelfRepository;

    public InventoryServiceImpl(BookRepository bookRepository,
                                CategoryRepository categoryRepository,
                                GenreRepository genreRepository,
                                BookItemRepository bookItemRepository,
                                BookDisposalRepository bookDisposalRepository,
                                ShelfRepository shelfRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.bookItemRepository = bookItemRepository;
        this.bookDisposalRepository = bookDisposalRepository;
        this.shelfRepository = shelfRepository;
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
        counts.put("Available", bookItemRepository.countByStatusIgnoreCase("Available"));
        counts.put("Borrowed", bookItemRepository.countByStatusIgnoreCase("Borrowed"));
        counts.put("Lost", bookItemRepository.countByStatusIgnoreCase("Lost"));
        counts.put("Damaged", bookItemRepository.countByStatusIgnoreCase("Damaged"));
        counts.put("Disposed", bookItemRepository.countByStatusIgnoreCase("Disposed"));
        return counts;
    }

    @Override
    public Map<String, Long> performInventoryAudit() {
        return getInventoryStatusCounts();
    }

    @Override
    public Book findBookById(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + bookId));
    }

    @Override
    public void addNewBook(String title, String isbn, Integer genreId, Integer quantity, String description, String coverImageUrl, Integer shelfId) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sách không được để trống.");
        }
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN không được để trống.");
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new IllegalArgumentException("Chọn thể loại hợp lệ."));

        Book book = new Book();
        book.setTitle(title.trim());
        book.setIsbn(isbn.trim());
        book.setGenre(genre);
        book.setStatus("Active");
        if (description != null && !description.trim().isEmpty()) {
            book.setDescription(description.trim());
        }
        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
            book.setCoverImageUrl(coverImageUrl.trim());
        }
        book = bookRepository.save(book);

        Shelf shelf = shelfId != null ? shelfRepository.findById(shelfId).orElse(null) : null;

        int copies = quantity != null && quantity > 0 ? quantity : 1;

        for (int i = 1; i <= copies; i++) {
            BookItem item = new BookItem();
            item.setBook(book);
            item.setShelf(shelf);
            item.setBarcode(
                    String.format(
                            "BC%03d-%03d",
                            book.getBookId(),
                            i
                    )
            );
            item.setStatus("Available");
            bookItemRepository.save(item);
        }
    }

    @Override
    public void updateBook(Integer bookId, String title, String isbn, Integer genreId, String status, String coverImageUrl, Integer shelfId) {
        Book book = findBookById(bookId);
        if (title != null && !title.trim().isEmpty()) {
            book.setTitle(title.trim());
        }
        if (isbn != null && !isbn.trim().isEmpty()) {
            book.setIsbn(isbn.trim());
        }
        if (genreId != null) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new IllegalArgumentException("Chọn thể loại hợp lệ."));
            book.setGenre(genre);
        }
        if (status != null && !status.trim().isEmpty()) {
            book.setStatus(status.trim());
        }
        // Chỉ cập nhật ảnh nếu có ảnh mới được upload
        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
            book.setCoverImageUrl(coverImageUrl.trim());
        }
        bookRepository.save(book);
        
        if (shelfId != null) {
            Shelf shelf = shelfRepository.findById(shelfId).orElse(null);
            List<BookItem> items = bookItemRepository.findByBook_BookId(bookId);
            for (BookItem item : items) {
                item.setShelf(shelf);
                bookItemRepository.save(item);
            }
        }
    }

    @Override
    public void updateBookStatus(Integer bookId, String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Chọn trạng thái sách.");
        }
        Book book = findBookById(bookId);
        book.setStatus(status.trim());
        bookRepository.save(book);
    }

    @Override
    public void removeBook(Integer bookId) {
        Book book = findBookById(bookId);
        // Xóa BookDisposals liên quan đến các BookItem của sách này
        List<BookItem> items = bookItemRepository.findByBook_BookId(bookId);
        for (BookItem item : items) {
            List<com.lms.entity.BookDisposal> disposals = bookDisposalRepository.findByBookItem(item);
            bookDisposalRepository.deleteAll(disposals);
        }
        // Xóa tất cả BookItems
        bookItemRepository.deleteAll(items);
        // Xóa sách
        bookRepository.delete(book);
    }

    @Override
    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        Category category = new Category();
        category.setCategoryName(name.trim());
        categoryRepository.save(category);
    }

    @Override
    public void addGenre(Integer categoryId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Chọn danh mục hợp lệ."));
        Genre genre = new Genre();
        genre.setCategory(category);
        genre.setGenreName(name.trim());
        genreRepository.save(genre);
    }
}
