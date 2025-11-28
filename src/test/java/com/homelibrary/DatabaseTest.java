package com.homelibrary;

import com.homelibrary.dao.BookDao;
import com.homelibrary.dao.Database;
import com.homelibrary.model.Author;
import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for database functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseTest {
    private static final String TEST_DB = "test_homelibrary.db";
    private BookDao bookDao;

    @BeforeAll
    static void setupTestDb() {
        // Use a test database
        System.setProperty("sqlite.db.file", TEST_DB);
    }

    @BeforeEach
    void setUp() {
        Database.getInstance();
        bookDao = new BookDao();
    }

    @AfterAll
    static void cleanup() {
        try {
            Database.getInstance().close();
            File dbFile = new File(TEST_DB);
            if (dbFile.exists()) {
                dbFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Database should initialize successfully")
    void testDatabaseInitialization() {
        assertNotNull(Database.getInstance());
        assertNotNull(Database.getInstance().getConnection());
    }

    @Test
    @Order(2)
    @DisplayName("Should create and save a book")
    void testCreateBook() throws SQLException {
        // Create a book
        Book book = new Book();
        book.setTitle("Test Book");
        book.setSubtitle("A Test Subtitle");

        // Add author
        Author author = new Author("Test Author");
        book.addAuthor(author);

        // Set category
        Category category = new Category("Test Category");
        book.setCategory(category);

        // Set other fields
        book.setIsbn13("9781234567890");
        book.setPublisher("Test Publisher");
        book.setYearPublished(2024);
        book.setLanguage("English");
        book.setFormat("Hardcover");
        book.setRead(false);
        book.setRating(5);

        // Save book
        Book savedBook = bookDao.save(book);

        // Verify
        assertNotNull(savedBook.getId());
        assertTrue(savedBook.getId() > 0);
        assertEquals("Test Book", savedBook.getTitle());
        assertEquals("Test Author", savedBook.getAuthors().get(0).getName());
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve all books")
    void testGetAllBooks() throws SQLException {
        var books = bookDao.findAll();
        assertNotNull(books);
        assertTrue(books.size() > 0);
    }

    @Test
    @Order(4)
    @DisplayName("Should search books by title")
    void testSearchBooks() throws SQLException {
        var books = bookDao.search("Test");
        assertNotNull(books);
        assertTrue(books.size() > 0);
        assertTrue(books.get(0).getTitle().contains("Test"));
    }

    @Test
    @Order(5)
    @DisplayName("Should update a book")
    void testUpdateBook() throws SQLException {
        // Get the first book
        var books = bookDao.findAll();
        assertTrue(books.size() > 0);

        Book book = books.get(0);
        String originalTitle = book.getTitle();

        // Update the title
        book.setTitle("Updated Test Book");
        Book updatedBook = bookDao.save(book);

        // Verify
        assertEquals(book.getId(), updatedBook.getId());
        assertEquals("Updated Test Book", updatedBook.getTitle());
        assertNotEquals(originalTitle, updatedBook.getTitle());
    }

    @Test
    @Order(6)
    @DisplayName("Should delete a book")
    void testDeleteBook() throws SQLException {
        // Get all books
        var books = bookDao.findAll();
        int initialCount = books.size();
        assertTrue(initialCount > 0);

        // Delete the first book
        Book book = books.get(0);
        boolean deleted = bookDao.delete(book.getId());

        // Verify
        assertTrue(deleted);

        var remainingBooks = bookDao.findAll();
        assertEquals(initialCount - 1, remainingBooks.size());
    }
}
