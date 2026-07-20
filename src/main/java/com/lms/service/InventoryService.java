package com.lms.service;

import com.lms.entity.Book;
import com.lms.entity.Category;
import com.lms.entity.Genre;
import java.util.List;
import java.util.Map;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
public interface InventoryService {

    List<Book> getAllBooks();

    List<Category> getAllCategories();

    List<Genre> getAllGenres();

    long countBooks();

    long countCategories();

    long countGenres();

    Map<String, Long> getInventoryStatusCounts();

    Map<String, Long> performInventoryAudit();

    Book findBookById(Integer bookId);

    void addNewBook(String title, String isbn, Integer genreId, Integer quantity, String description, String coverImageUrl, Integer shelfId, String bookCondition, String authorName);

    void addBookCopies(Integer bookId, Integer quantity, Integer shelfId, String bookCondition);

    void deleteBookCopies(Integer bookId, List<Integer> bookItemIds);

    void updateBook(Integer bookId, String title, String isbn, Integer genreId, String status, String coverImageUrl, Integer shelfId, String description, String author);

    void updateBookStatus(Integer bookId, String status);

    void removeBook(Integer bookId);

    void addCategory(String name);

    void addGenre(Integer categoryId, String name);

    void updateCategory(Integer categoryId, String newName);

    void updateGenre(Integer genreId, String newName, Integer newCategoryId);

    void deleteGenre(Integer genreId);

    void deleteCategory(Integer categoryId);
}
