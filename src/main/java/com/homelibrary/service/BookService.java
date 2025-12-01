package com.homelibrary.service;

import com.homelibrary.dao.AuthorDao;
import com.homelibrary.dao.BookDao;
import com.homelibrary.dao.CategoryDao;
import com.homelibrary.model.Author;
import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for book-related business logic.
 */
public class BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    private final BookDao bookDao;
    private final AuthorDao authorDao;
    private final CategoryDao categoryDao;
    private final ConfigService configService;

    public BookService() {
        this.bookDao = new BookDao();
        this.authorDao = new AuthorDao();
        this.categoryDao = new CategoryDao();
        this.configService = ConfigService.getInstance();

        // Ensure covers directory exists
        createCoversDirectory();
    }

    /**
     * Create covers directory if it doesn't exist.
     */
    private void createCoversDirectory() {
        String coversDir = configService.getCoversDirectory();
        File dir = new File(coversDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("Created covers directory: {}", coversDir);
            }
        }
    }

    /**
     * Save a book.
     */
    public Book saveBook(Book book) throws SQLException {
        // Ensure category is saved if needed
        if (book.getCategory() != null && book.getCategory().getId() == null) {
            Category category = categoryDao.getOrCreate(book.getCategory().getName());
            book.setCategory(category);
        }

        // Ensure authors are saved if needed
        if (book.getAuthors() != null) {
            for (int i = 0; i < book.getAuthors().size(); i++) {
                Author author = book.getAuthors().get(i);
                if (author.getId() == null) {
                    author = authorDao.getOrCreate(author.getName());
                    book.getAuthors().set(i, author);
                }
            }
        }

        return bookDao.save(book);
    }

    /**
     * Find book by ID.
     */
    public Optional<Book> findBookById(Integer id) throws SQLException {
        return bookDao.findById(id);
    }

    /**
     * Get all books.
     */
    public List<Book> getAllBooks() throws SQLException {
        return bookDao.findAll();
    }

    /**
     * Search books.
     */
    public List<Book> searchBooks(String query) throws SQLException {
        return bookDao.search(query);
    }

    /**
     * Find books by category.
     */
    public List<Book> getBooksByCategory(Integer categoryId) throws SQLException {
        return bookDao.findByCategory(categoryId);
    }

    /**
     * Find books by read status.
     */
    public List<Book> getBooksByReadStatus(boolean isRead) throws SQLException {
        return bookDao.findByReadStatus(isRead);
    }

    /**
     * Find books by borrowed status.
     */
    public List<Book> getBooksByBorrowedStatus(boolean isBorrowed) throws SQLException {
        return bookDao.findByBorrowedStatus(isBorrowed);
    }

    /**
     * Delete a book.
     */
    public boolean deleteBook(Integer id) throws SQLException {
        Optional<Book> book = bookDao.findById(id);

        if (book.isPresent() && book.get().getCoverImagePath() != null) {
            // Delete cover image file
            deleteCoverImage(book.get().getCoverImagePath());
        }

        return bookDao.delete(id);
    }

    /**
     * Get all categories.
     */
    public List<Category> getAllCategories() throws SQLException {
        return categoryDao.findAll();
    }

    /**
     * Save category.
     */
    public Category saveCategory(Category category) throws SQLException {
        return categoryDao.save(category);
    }

    /**
     * Get all authors.
     */
    public List<Author> getAllAuthors() throws SQLException {
        return authorDao.findAll();
    }

    /**
     * Save author.
     */
    public Author saveAuthor(Author author) throws SQLException {
        return authorDao.save(author);
    }

    /**
     * Upload cover image for a book.
     */
    public String uploadCoverImage(File sourceFile, Integer bookId) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Source file does not exist");
        }

        String coversDir = configService.getCoversDirectory();
        String extension = getFileExtension(sourceFile.getName());
        String fileName = bookId + extension;
        Path targetPath = Paths.get(coversDir, fileName);

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Uploaded cover image: {}", targetPath);

        return targetPath.toString();
    }

    /**
     * Delete cover image file.
     */
    private void deleteCoverImage(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
            logger.info("Deleted cover image: {}", imagePath);
        } catch (IOException e) {
            logger.error("Failed to delete cover image: {}", imagePath, e);
        }
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".jpg";
    }

    /**
     * Get total book count.
     */
    public int getBookCount() throws SQLException {
        return bookDao.count();
    }

    /**
     * Get statistics about the library.
     */
    public LibraryStats getLibraryStats() throws SQLException {
        LibraryStats stats = new LibraryStats();
        stats.totalBooks = bookDao.count();
        stats.readBooks = bookDao.findByReadStatus(true).size();
        stats.unreadBooks = bookDao.findByReadStatus(false).size();
        stats.borrowedBooks = bookDao.findByBorrowedStatus(true).size();
        stats.totalAuthors = authorDao.findAll().size();
        stats.totalCategories = categoryDao.findAll().size();
        return stats;
    }

    /**
     * Inner class for library statistics.
     */
    public static class LibraryStats {
        public int totalBooks;
        public int readBooks;
        public int unreadBooks;
        public int borrowedBooks;
        public int totalAuthors;
        public int totalCategories;

        @Override
        public String toString() {
            return String.format("Total: %d books, Read: %d, Unread: %d, Borrowed: %d, Authors: %d, Categories: %d",
                    totalBooks, readBooks, unreadBooks, borrowedBooks, totalAuthors, totalCategories);
        }
    }
}
