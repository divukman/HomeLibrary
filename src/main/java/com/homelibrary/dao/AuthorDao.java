package com.homelibrary.dao;

import com.homelibrary.model.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Author operations.
 */
public class AuthorDao {
    private static final Logger logger = LoggerFactory.getLogger(AuthorDao.class);
    private final Database database;

    public AuthorDao() {
        this.database = Database.getInstance();
    }

    /**
     * Save a new author or update existing one.
     */
    public Author save(Author author) throws SQLException {
        if (author.getId() == null) {
            return insert(author);
        } else {
            return update(author);
        }
    }

    /**
     * Insert a new author.
     */
    private Author insert(Author author) throws SQLException {
        String sql = "INSERT INTO author (name) VALUES (?)";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, author.getName());
            pstmt.executeUpdate();

            // Get the last inserted ID using SQLite-specific query
            try (Statement stmt = database.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    author.setId(rs.getInt(1));
                    logger.debug("Inserted author: {}", author);
                }
            }
        }

        return author;
    }

    /**
     * Update an existing author.
     */
    private Author update(Author author) throws SQLException {
        String sql = "UPDATE author SET name = ? WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, author.getName());
            pstmt.setInt(2, author.getId());
            pstmt.executeUpdate();
            logger.debug("Updated author: {}", author);
        }

        return author;
    }

    /**
     * Find author by ID.
     */
    public Optional<Author> findById(Integer id) throws SQLException {
        String sql = "SELECT id, name FROM author WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToAuthor(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find author by name.
     */
    public Optional<Author> findByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM author WHERE name = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToAuthor(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find all authors.
     */
    public List<Author> findAll() throws SQLException {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT id, name FROM author ORDER BY name";

        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                authors.add(mapResultSetToAuthor(rs));
            }
        }

        logger.debug("Found {} authors", authors.size());
        return authors;
    }

    /**
     * Find authors for a specific book.
     */
    public List<Author> findByBookId(Integer bookId) throws SQLException {
        List<Author> authors = new ArrayList<>();
        String sql = """
            SELECT a.id, a.name
            FROM author a
            INNER JOIN book_author ba ON a.id = ba.authorId
            WHERE ba.bookId = ?
            ORDER BY a.name
            """;

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                authors.add(mapResultSetToAuthor(rs));
            }
        }

        return authors;
    }

    /**
     * Delete author by ID.
     */
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM author WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            logger.debug("Deleted author with id: {}", id);
            return affected > 0;
        }
    }

    /**
     * Get or create author by name.
     */
    public Author getOrCreate(String name) throws SQLException {
        Optional<Author> existing = findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        Author author = new Author(name);
        return insert(author);
    }

    /**
     * Map ResultSet to Author object.
     */
    private Author mapResultSetToAuthor(ResultSet rs) throws SQLException {
        Author author = new Author();
        author.setId(rs.getInt("id"));
        author.setName(rs.getString("name"));
        return author;
    }
}
