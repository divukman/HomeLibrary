package com.homelibrary.dao;

import com.homelibrary.model.Author;
import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Book operations.
 */
public class BookDao {
    private static final Logger logger = LoggerFactory.getLogger(BookDao.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Database database;
    private final AuthorDao authorDao;
    private final CategoryDao categoryDao;

    public BookDao() {
        this.database = Database.getInstance();
        this.authorDao = new AuthorDao();
        this.categoryDao = new CategoryDao();
    }

    /**
     * Save a new book or update existing one.
     */
    public Book save(Book book) throws SQLException {
        Connection conn = database.getConnection();
        try {
            conn.setAutoCommit(false);

            if (book.getId() == null) {
                book = insert(book);
            } else {
                book = update(book);
            }

            // Handle authors relationship
            saveBookAuthors(book);

            conn.commit();
            logger.debug("Saved book: {}", book);
            return book;
        } catch (SQLException e) {
            conn.rollback();
            logger.error("Failed to save book", e);
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Insert a new book.
     */
    private Book insert(Book book) throws SQLException {
        String sql = """
            INSERT INTO book (title, subtitle, isbn10, isbn13, publisher, yearPublished,
                             categoryId, shelfLocation, tags, format, language, notes,
                             dateAdded, isRead, rating, coverImagePath, amazonAsin,
                             physicalLocation, isBorrowed, borrowedTo, borrowedDate)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            setPreparedStatementParameters(pstmt, book);
            pstmt.executeUpdate();

            // Get the last inserted ID using SQLite-specific query
            try (Statement stmt = database.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    book.setId(rs.getInt(1));
                }
            }
        }

        return book;
    }

    /**
     * Update an existing book.
     */
    private Book update(Book book) throws SQLException {
        String sql = """
            UPDATE book SET title = ?, subtitle = ?, isbn10 = ?, isbn13 = ?, publisher = ?,
                           yearPublished = ?, categoryId = ?, shelfLocation = ?, tags = ?,
                           format = ?, language = ?, notes = ?, dateAdded = ?, isRead = ?,
                           rating = ?, coverImagePath = ?, amazonAsin = ?,
                           physicalLocation = ?, isBorrowed = ?, borrowedTo = ?, borrowedDate = ?
            WHERE id = ?
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            setPreparedStatementParameters(pstmt, book);
            pstmt.setInt(22, book.getId());
            pstmt.executeUpdate();
        }

        return book;
    }

    /**
     * Set common prepared statement parameters for insert and update.
     */
    private void setPreparedStatementParameters(PreparedStatement pstmt, Book book) throws SQLException {
        pstmt.setString(1, book.getTitle());
        pstmt.setString(2, book.getSubtitle());
        pstmt.setString(3, book.getIsbn10());
        pstmt.setString(4, book.getIsbn13());
        pstmt.setString(5, book.getPublisher());
        pstmt.setObject(6, book.getYearPublished());
        pstmt.setObject(7, book.getCategory() != null ? book.getCategory().getId() : null);
        pstmt.setString(8, book.getShelfLocation());
        pstmt.setString(9, book.getTags());
        pstmt.setString(10, book.getFormat());
        pstmt.setString(11, book.getLanguage());
        pstmt.setString(12, book.getNotes());
        pstmt.setString(13, book.getDateAdded() != null ? book.getDateAdded().format(DATE_FORMATTER) : null);
        pstmt.setInt(14, book.isRead() ? 1 : 0);
        pstmt.setObject(15, book.getRating());
        pstmt.setString(16, book.getCoverImagePath());
        pstmt.setString(17, book.getAmazonAsin());
        pstmt.setString(18, book.getPhysicalLocation());
        pstmt.setInt(19, book.isBorrowed() ? 1 : 0);
        pstmt.setString(20, book.getBorrowedTo());
        pstmt.setString(21, book.getBorrowedDate() != null ? book.getBorrowedDate().format(DATE_FORMATTER) : null);
    }

    /**
     * Save book-author relationships.
     */
    private void saveBookAuthors(Book book) throws SQLException {
        if (book.getId() == null) {
            return;
        }

        // Delete existing relationships
        String deleteSql = "DELETE FROM book_author WHERE bookId = ?";
        try (PreparedStatement pstmt = database.getConnection().prepareStatement(deleteSql)) {
            pstmt.setInt(1, book.getId());
            pstmt.executeUpdate();
        }

        // Insert new relationships
        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            String insertSql = "INSERT INTO book_author (bookId, authorId) VALUES (?, ?)";
            try (PreparedStatement pstmt = database.getConnection().prepareStatement(insertSql)) {
                for (Author author : book.getAuthors()) {
                    // Ensure author is saved
                    if (author.getId() == null) {
                        author = authorDao.save(author);
                    }

                    pstmt.setInt(1, book.getId());
                    pstmt.setInt(2, author.getId());
                    pstmt.executeUpdate();
                }
            }
        }
    }

    /**
     * Find book by ID.
     */
    public Optional<Book> findById(Integer id) throws SQLException {
        String sql = """
            SELECT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            WHERE b.id = ?
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Book book = mapResultSetToBook(rs);
                // Load authors
                book.setAuthors(authorDao.findByBookId(book.getId()));
                return Optional.of(book);
            }
        }

        return Optional.empty();
    }

    /**
     * Find all books.
     */
    public List<Book> findAll() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            ORDER BY b.title
            """;

        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                // Load authors for each book
                book.setAuthors(authorDao.findByBookId(book.getId()));
                books.add(book);
            }
        }

        logger.debug("Found {} books", books.size());
        return books;
    }

    /**
     * Search books by title, author, ISBN, category, tags, physical location, or borrower.
     */
    public List<Book> search(String query) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT DISTINCT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            LEFT JOIN book_author ba ON b.id = ba.bookId
            LEFT JOIN author a ON ba.authorId = a.id
            WHERE b.title LIKE ? OR b.subtitle LIKE ?
               OR b.isbn10 LIKE ? OR b.isbn13 LIKE ?
               OR a.name LIKE ?
               OR c.name LIKE ?
               OR b.tags LIKE ?
               OR b.publisher LIKE ?
               OR b.physicalLocation LIKE ?
               OR b.borrowedTo LIKE ?
            ORDER BY b.title
            """;

        String searchPattern = "%" + query + "%";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setString(5, searchPattern);
            pstmt.setString(6, searchPattern);
            pstmt.setString(7, searchPattern);
            pstmt.setString(8, searchPattern);
            pstmt.setString(9, searchPattern);
            pstmt.setString(10, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                book.setAuthors(authorDao.findByBookId(book.getId()));
                books.add(book);
            }
        }

        return books;
    }

    /**
     * Find books by category.
     */
    public List<Book> findByCategory(Integer categoryId) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            WHERE b.categoryId = ?
            ORDER BY b.title
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                book.setAuthors(authorDao.findByBookId(book.getId()));
                books.add(book);
            }
        }

        return books;
    }

    /**
     * Find books by read status.
     */
    public List<Book> findByReadStatus(boolean isRead) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            WHERE b.isRead = ?
            ORDER BY b.title
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, isRead ? 1 : 0);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                book.setAuthors(authorDao.findByBookId(book.getId()));
                books.add(book);
            }
        }

        return books;
    }

    /**
     * Delete book by ID.
     */
    public boolean delete(Integer id) throws SQLException {
        Connection conn = database.getConnection();
        try {
            conn.setAutoCommit(false);

            // Delete book-author relationships first
            String deleteRelSql = "DELETE FROM book_author WHERE bookId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteRelSql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }

            // Delete book
            String deleteSql = "DELETE FROM book WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, id);
                int affected = pstmt.executeUpdate();

                conn.commit();
                logger.debug("Deleted book with id: {}", id);
                return affected > 0;
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Map ResultSet to Book object.
     */
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("id"));
        book.setTitle(rs.getString("title"));
        book.setSubtitle(rs.getString("subtitle"));
        book.setIsbn10(rs.getString("isbn10"));
        book.setIsbn13(rs.getString("isbn13"));
        book.setPublisher(rs.getString("publisher"));

        Integer yearPublished = rs.getInt("yearPublished");
        book.setYearPublished(rs.wasNull() ? null : yearPublished);

        book.setShelfLocation(rs.getString("shelfLocation"));
        book.setTags(rs.getString("tags"));
        book.setFormat(rs.getString("format"));
        book.setLanguage(rs.getString("language"));
        book.setNotes(rs.getString("notes"));

        String dateAddedStr = rs.getString("dateAdded");
        if (dateAddedStr != null) {
            book.setDateAdded(LocalDateTime.parse(dateAddedStr, DATE_FORMATTER));
        }

        book.setRead(rs.getInt("isRead") == 1);

        Integer rating = rs.getInt("rating");
        book.setRating(rs.wasNull() ? null : rating);

        book.setCoverImagePath(rs.getString("coverImagePath"));
        book.setAmazonAsin(rs.getString("amazonAsin"));

        // Map new fields
        book.setPhysicalLocation(rs.getString("physicalLocation"));
        book.setBorrowed(rs.getInt("isBorrowed") == 1);
        book.setBorrowedTo(rs.getString("borrowedTo"));

        String borrowedDateStr = rs.getString("borrowedDate");
        if (borrowedDateStr != null) {
            book.setBorrowedDate(LocalDateTime.parse(borrowedDateStr, DATE_FORMATTER));
        }

        // Map category if present
        Integer catId = rs.getInt("cat_id");
        if (!rs.wasNull()) {
            Category category = new Category(catId, rs.getString("cat_name"));
            book.setCategory(category);
        }

        return book;
    }

    /**
     * Get count of all books.
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM book";
        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Find books by borrowed status.
     */
    public List<Book> findByBorrowedStatus(boolean isBorrowed) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.*, c.id as cat_id, c.name as cat_name
            FROM book b
            LEFT JOIN category c ON b.categoryId = c.id
            WHERE b.isBorrowed = ?
            ORDER BY b.borrowedDate DESC
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, isBorrowed ? 1 : 0);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                book.setAuthors(authorDao.findByBookId(book.getId()));
                books.add(book);
            }
        }

        return books;
    }
}
