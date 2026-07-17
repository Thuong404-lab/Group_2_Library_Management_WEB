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
import com.lms.repository.BookRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.ShelfRepository;
import com.lms.service.InventoryService;
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
    private static final String STATUS_LOST = "Lost";
    private static final String STATUS_DAMAGED = "Damaged";
    private static final String STATUS_DISPOSED = "Disposed";
    private static final String STATUS_ACTIVE = "Active";

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final BookItemRepository bookItemRepository;
    private final ShelfRepository shelfRepository;
    private final AuthorRepository authorRepository;

    public InventoryServiceImpl(BookRepository bookRepository,
            CategoryRepository categoryRepository,
            GenreRepository genreRepository,
            BookItemRepository bookItemRepository,
            ShelfRepository shelfRepository,
            AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.bookItemRepository = bookItemRepository;
        this.shelfRepository = shelfRepository;
        this.authorRepository = authorRepository;
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
        counts.put(STATUS_LOST, bookItemRepository.countByStatusIgnoreCase(STATUS_LOST));
        counts.put(STATUS_DAMAGED, bookItemRepository.countByStatusIgnoreCase(STATUS_DAMAGED));
        counts.put(STATUS_DISPOSED, bookItemRepository.countByStatusIgnoreCase(STATUS_DISPOSED));
        return counts;
    }

    @Override
    public Map<String, Long> performInventoryAudit() {
        return getInventoryStatusCounts();
    }

    @Override
    public Book findBookById(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + bookId));
    }

    @Override
    public void addNewBook(String title, String isbn, Integer genreId, Integer quantity, String description,
            String coverImageUrl, Integer shelfId, String bookCondition, String authorName) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Tên sách không được để trống.");
        }
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new ValidationException("ISBN không được để trống.");
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ValidationException("Chọn thể loại hợp lệ."));

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
                            i));
            item.setStatus(STATUS_AVAILABLE);
            if (bookCondition != null && !bookCondition.trim().isEmpty()) {
                item.setBookCondition(bookCondition.trim());
            }

            bookItemRepository.save(item);
        }
    }

    @Override
    public void updateBook(Integer bookId, String title, String isbn, Integer genreId, String status,
            String coverImageUrl, Integer shelfId, String description, String authorName) {
        Book book = findBookById(bookId);
        if (title != null && !title.trim().isEmpty()) {
            book.setTitle(title.trim());
        }
        if (isbn != null && !isbn.trim().isEmpty()) {
            book.setIsbn(isbn.trim());
        }
        if (genreId != null) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new ValidationException("Chọn thể loại hợp lệ."));
            book.setGenre(genre);
        }
        if (status != null && !status.trim().isEmpty()) {
            book.setStatus(status.trim());
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
            throw new ValidationException("Chọn trạng thái sách.");
        }
        Book book = findBookById(bookId);
        book.setStatus(status.trim());
        bookRepository.save(book);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void removeBook(Integer bookId) {
        Book book = findBookById(bookId);
        List<BookItem> items = bookItemRepository.findByBook_BookId(bookId);
        for (BookItem item : items) {
            if (STATUS_BORROWED.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException("Không thể xóa sách vì có bản sách đang được mượn.");
            }
        }
        try {
            // Xóa tất cả BookItems
            bookItemRepository.deleteAll(items);
            // Xóa sách
            bookRepository.delete(book);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Không thể xóa sách này vì sách đã từng có lịch sử giao dịch (mượn/trả) trong hệ thống.", ex);
        }
    }

    @Override
    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Tên danh mục không được để trống.");
        }
        Category category = new Category();
        category.setCategoryName(name.trim());
        categoryRepository.save(category);
    }

    @Override
    public void addGenre(Integer categoryId, String name) {
        if (categoryId == null) {
            throw new ValidationException("Vui lòng chọn danh mục.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Tên thể loại không được để trống.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại."));

        Genre genre = new Genre();
        genre.setCategory(category);
        genre.setGenreName(name.trim());
        genreRepository.save(genre);
    }

    @Override
    public void updateCategory(Integer categoryId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Tên danh mục không được để trống.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục."));
        category.setCategoryName(newName.trim());
        categoryRepository.save(category);
    }

    @Override
    public void updateGenre(Integer genreId, String newName, Integer newCategoryId) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Tên thể loại không được để trống.");
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại."));

        if (newCategoryId != null) {
            Category category = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại."));
            genre.setCategory(category);
        }

        genre.setGenreName(newName.trim());
        genreRepository.save(genre);
    }

    @Override
    public void deleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại."));

        if (bookRepository.existsByGenre_GenreId(genreId)) {
            throw new ConflictException("Không thể xóa thể loại vì đang có sách thuộc thể loại này.");
        }

        genreRepository.delete(genre);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục."));
        
        if (genreRepository.existsByCategory_CategoryId(categoryId)) {
            throw new ConflictException("Không thể xóa danh mục vì đang có thể loại thuộc danh mục này.");
        }
        categoryRepository.delete(category);
    }
}
