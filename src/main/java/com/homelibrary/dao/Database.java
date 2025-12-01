package com.homelibrary.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection and initialization.
 */
public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String DB_FILE = "homelibrary.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static Database instance;
    private Connection connection;

    private Database() {
        try {
            // Ensure SQLite JDBC driver is loaded
            Class.forName("org.sqlite.JDBC");
            connect();
            initializeSchema();
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Get singleton instance of Database.
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Establish database connection.
     */
    private void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            logger.info("Connected to database: {}", DB_FILE);
        } catch (SQLException e) {
            logger.error("Failed to connect to database", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    /**
     * Get active database connection.
     */
    public Connection getConnection() {
        try {
            // Check if connection is valid, reconnect if necessary
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            logger.warn("Connection check failed, attempting to reconnect", e);
            connect();
        }
        return connection;
    }

    /**
     * Initialize database schema if tables don't exist.
     */
    private void initializeSchema() {
        logger.info("Initializing database schema");

        String[] schemaSql = {
            // Category table
            """
            CREATE TABLE IF NOT EXISTS category (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
            """,

            // Author table
            """
            CREATE TABLE IF NOT EXISTS author (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )
            """,

            // Book table
            """
            CREATE TABLE IF NOT EXISTS book (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                subtitle TEXT,
                isbn10 TEXT,
                isbn13 TEXT,
                publisher TEXT,
                yearPublished INTEGER,
                categoryId INTEGER,
                shelfLocation TEXT,
                tags TEXT,
                format TEXT,
                language TEXT,
                notes TEXT,
                dateAdded TEXT DEFAULT CURRENT_TIMESTAMP,
                isRead INTEGER DEFAULT 0,
                rating INTEGER,
                coverImagePath TEXT,
                amazonAsin TEXT,
                physicalLocation TEXT,
                isBorrowed INTEGER DEFAULT 0,
                borrowedTo TEXT,
                borrowedDate TEXT,
                FOREIGN KEY(categoryId) REFERENCES category(id)
            )
            """,

            // Book-Author many-to-many relationship
            """
            CREATE TABLE IF NOT EXISTS book_author (
                bookId INTEGER,
                authorId INTEGER,
                PRIMARY KEY (bookId, authorId),
                FOREIGN KEY(bookId) REFERENCES book(id) ON DELETE CASCADE,
                FOREIGN KEY(authorId) REFERENCES author(id) ON DELETE CASCADE
            )
            """,

            // Indexes for better query performance
            "CREATE INDEX IF NOT EXISTS idx_book_title ON book(title)",
            "CREATE INDEX IF NOT EXISTS idx_book_isbn10 ON book(isbn10)",
            "CREATE INDEX IF NOT EXISTS idx_book_isbn13 ON book(isbn13)",
            "CREATE INDEX IF NOT EXISTS idx_author_name ON author(name)",
            "CREATE INDEX IF NOT EXISTS idx_book_author_book ON book_author(bookId)",
            "CREATE INDEX IF NOT EXISTS idx_book_author_author ON book_author(authorId)"
        };

        try (Statement stmt = getConnection().createStatement()) {
            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");

            // Execute each schema statement
            for (String sql : schemaSql) {
                stmt.execute(sql);
            }

            logger.info("Database schema initialized successfully");

            // Run migrations for existing databases
            runMigrations();
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    /**
     * Run database migrations to add new columns to existing databases.
     */
    private void runMigrations() {
        logger.info("Running database migrations");

        try (Statement stmt = getConnection().createStatement()) {
            // Migration: Add physicalLocation, isBorrowed, borrowedTo, borrowedDate columns
            String[] migrations = {
                "ALTER TABLE book ADD COLUMN physicalLocation TEXT",
                "ALTER TABLE book ADD COLUMN isBorrowed INTEGER DEFAULT 0",
                "ALTER TABLE book ADD COLUMN borrowedTo TEXT",
                "ALTER TABLE book ADD COLUMN borrowedDate TEXT"
            };

            for (String migration : migrations) {
                try {
                    stmt.execute(migration);
                    logger.info("Migration executed: {}", migration);
                } catch (SQLException e) {
                    // Column already exists, ignore
                    if (!e.getMessage().contains("duplicate column name")) {
                        throw e;
                    }
                }
            }

            logger.info("Database migrations completed successfully");
        } catch (SQLException e) {
            logger.error("Failed to run migrations", e);
            throw new RuntimeException("Migration failed", e);
        }
    }

    /**
     * Close database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Check if database file exists.
     */
    public static boolean databaseExists() {
        return new File(DB_FILE).exists();
    }
}
