package com.homelibrary.dao;

import com.homelibrary.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Category operations.
 */
public class CategoryDao {
    private static final Logger logger = LoggerFactory.getLogger(CategoryDao.class);
    private final Database database;

    public CategoryDao() {
        this.database = Database.getInstance();
    }

    /**
     * Save a new category or update existing one.
     */
    public Category save(Category category) throws SQLException {
        if (category.getId() == null) {
            return insert(category);
        } else {
            return update(category);
        }
    }

    /**
     * Insert a new category.
     */
    private Category insert(Category category) throws SQLException {
        String sql = "INSERT INTO category (name) VALUES (?)";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.executeUpdate();

            // Get the last inserted ID using SQLite-specific query
            try (Statement stmt = database.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                    logger.debug("Inserted category: {}", category);
                }
            }
        }

        return category;
    }

    /**
     * Update an existing category.
     */
    private Category update(Category category) throws SQLException {
        String sql = "UPDATE category SET name = ? WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setInt(2, category.getId());
            pstmt.executeUpdate();
            logger.debug("Updated category: {}", category);
        }

        return category;
    }

    /**
     * Find category by ID.
     */
    public Optional<Category> findById(Integer id) throws SQLException {
        String sql = "SELECT id, name FROM category WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCategory(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find category by name.
     */
    public Optional<Category> findByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM category WHERE name = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCategory(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find all categories.
     */
    public List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM category ORDER BY name";

        try (Statement stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        }

        logger.debug("Found {} categories", categories.size());
        return categories;
    }

    /**
     * Delete category by ID.
     */
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM category WHERE id = ?";

        try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            logger.debug("Deleted category with id: {}", id);
            return affected > 0;
        }
    }

    /**
     * Get or create category by name.
     */
    public Category getOrCreate(String name) throws SQLException {
        Optional<Category> existing = findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        Category category = new Category(name);
        return insert(category);
    }

    /**
     * Map ResultSet to Category object.
     */
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        return category;
    }
}
