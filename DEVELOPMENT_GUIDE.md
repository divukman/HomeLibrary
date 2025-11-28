# Development Guide

This guide helps developers understand and extend the Home Library Manager application.

## Architecture Overview

The application follows a clean 4-layer architecture:

```
UI Layer → Service Layer → DAO Layer → Database
```

## Adding New Features

### 1. Adding a New Field to Books

**Step 1: Update Database Schema**

Edit `Database.java` and add the column to the schema:

```java
"""
CREATE TABLE IF NOT EXISTS book (
    ...
    myNewField TEXT,
    ...
)
"""
```

**Step 2: Update Book Model**

Edit `Book.java`:

```java
private String myNewField;

public String getMyNewField() {
    return myNewField;
}

public void setMyNewField(String myNewField) {
    this.myNewField = myNewField;
}
```

**Step 3: Update BookDao**

Add to insert/update SQL in `BookDao.java`:

```java
private void setPreparedStatementParameters(PreparedStatement pstmt, Book book) {
    // ... existing parameters
    pstmt.setString(18, book.getMyNewField());
}
```

And in mapping:

```java
private Book mapResultSetToBook(ResultSet rs) {
    // ... existing mapping
    book.setMyNewField(rs.getString("myNewField"));
    return book;
}
```

**Step 4: Update UI**

Add field to `BookFormView.java`:

```java
private TextField myNewFieldField;

// In createForm():
grid.add(new Label("My Field:"), 0, row);
myNewFieldField = new TextField();
grid.add(myNewFieldField, 1, row);
row++;

// In populateFields():
myNewFieldField.setText(book.getMyNewField());

// In saveBook():
book.setMyNewField(myNewFieldField.getText().trim());
```

### 2. Adding a New Entity (e.g., Publisher)

**Step 1: Create Model**

Create `src/main/java/com/homelibrary/model/Publisher.java`:

```java
package com.homelibrary.model;

import java.util.Objects;

public class Publisher {
    private Integer id;
    private String name;
    private String country;

    public Publisher() {}

    public Publisher(String name) {
        this.name = name;
    }

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Publisher publisher = (Publisher) o;
        return Objects.equals(id, publisher.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
```

**Step 2: Create DAO**

Create `src/main/java/com/homelibrary/dao/PublisherDao.java`:

```java
package com.homelibrary.dao;

import com.homelibrary.model.Publisher;
import java.sql.*;
import java.util.Optional;

public class PublisherDao {
    private final Database database;

    public PublisherDao() {
        this.database = Database.getInstance();
    }

    public Publisher save(Publisher publisher) throws SQLException {
        if (publisher.getId() == null) {
            return insert(publisher);
        } else {
            return update(publisher);
        }
    }

    private Publisher insert(Publisher publisher) throws SQLException {
        String sql = "INSERT INTO publisher (name, country) VALUES (?, ?)";

        try (PreparedStatement pstmt = database.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, publisher.getName());
            pstmt.setString(2, publisher.getCountry());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                publisher.setId(rs.getInt(1));
            }
        }
        return publisher;
    }

    // Add other CRUD methods...
}
```

**Step 3: Add to Database Schema**

Update `Database.java`:

```java
"""
CREATE TABLE IF NOT EXISTS publisher (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    country TEXT
)
"""
```

### 3. Adding Import/Export Functionality

**Step 1: Create Export Service**

Create `src/main/java/com/homelibrary/service/ExportService.java`:

```java
package com.homelibrary.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.homelibrary.model.Book;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {
    private final BookService bookService;
    private final Gson gson;

    public ExportService() {
        this.bookService = new BookService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void exportToJson(String filePath) throws Exception {
        List<Book> books = bookService.getAllBooks();

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(books, writer);
        }
    }

    public void exportToCsv(String filePath) throws Exception {
        List<Book> books = bookService.getAllBooks();

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("ID,Title,Authors,ISBN-13,Publisher,Year\n");

            // Write data
            for (Book book : books) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s\n",
                    book.getId(),
                    escapeCsv(book.getTitle()),
                    escapeCsv(book.getAuthorsString()),
                    escapeCsv(book.getIsbn13()),
                    escapeCsv(book.getPublisher()),
                    book.getYearPublished()
                ));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
```

**Step 2: Add Menu Items**

Update `MainApp.java`:

```java
Menu fileMenu = new Menu("File");

MenuItem exportJsonItem = new MenuItem("Export to JSON...");
exportJsonItem.setOnAction(e -> handleExportJson());

MenuItem exportCsvItem = new MenuItem("Export to CSV...");
exportCsvItem.setOnAction(e -> handleExportCsv());

fileMenu.getItems().addAll(
    exportJsonItem,
    exportCsvItem,
    new SeparatorMenuItem(),
    exitItem
);
```

### 4. Adding Custom Reports

Create `src/main/java/com/homelibrary/service/ReportService.java`:

```java
package com.homelibrary.service;

import com.homelibrary.dao.BookDao;
import com.homelibrary.model.Book;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final BookDao bookDao;

    public ReportService() {
        this.bookDao = new BookDao();
    }

    /**
     * Get books grouped by category.
     */
    public Map<String, List<Book>> getBooksByCategory() throws SQLException {
        List<Book> books = bookDao.findAll();

        return books.stream()
            .collect(Collectors.groupingBy(
                book -> book.getCategory() != null ?
                    book.getCategory().getName() : "Uncategorized"
            ));
    }

    /**
     * Get books grouped by year.
     */
    public Map<Integer, List<Book>> getBooksByYear() throws SQLException {
        List<Book> books = bookDao.findAll();

        return books.stream()
            .filter(book -> book.getYearPublished() != null)
            .collect(Collectors.groupingBy(Book::getYearPublished));
    }

    /**
     * Get reading statistics.
     */
    public ReadingStats getReadingStats() throws SQLException {
        List<Book> books = bookDao.findAll();
        ReadingStats stats = new ReadingStats();

        stats.totalBooks = books.size();
        stats.readBooks = (int) books.stream().filter(Book::isRead).count();
        stats.unreadBooks = stats.totalBooks - stats.readBooks;

        // Average rating
        OptionalDouble avgRating = books.stream()
            .filter(b -> b.getRating() != null && b.getRating() > 0)
            .mapToInt(Book::getRating)
            .average();
        stats.averageRating = avgRating.isPresent() ? avgRating.getAsDouble() : 0.0;

        return stats;
    }

    public static class ReadingStats {
        public int totalBooks;
        public int readBooks;
        public int unreadBooks;
        public double averageRating;
    }
}
```

## Common Code Patterns

### Safe Database Operations

Always use try-catch and handle SQLExceptions:

```java
try {
    Book book = bookService.saveBook(newBook);
    logger.info("Saved book: {}", book);
} catch (SQLException e) {
    logger.error("Failed to save book", e);
    showErrorAlert("Error", "Failed to save: " + e.getMessage());
}
```

### Using Transactions

```java
Connection conn = database.getConnection();
try {
    conn.setAutoCommit(false);

    // Multiple operations
    operation1();
    operation2();

    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```

### JavaFX Alert Dialogs

```java
// Information
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Success");
alert.setContentText("Operation completed!");
alert.showAndWait();

// Confirmation
Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
confirm.setTitle("Confirm");
confirm.setContentText("Are you sure?");
Optional<ButtonType> result = confirm.showAndWait();
if (result.isPresent() && result.get() == ButtonType.OK) {
    // User clicked OK
}
```

## Testing

### Writing Unit Tests

```java
@Test
@DisplayName("Should create a book with authors")
void testCreateBookWithAuthors() throws SQLException {
    // Arrange
    Book book = new Book();
    book.setTitle("Test Book");
    book.addAuthor(new Author("John Doe"));

    // Act
    Book saved = bookDao.save(book);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(1, saved.getAuthors().size());
    assertEquals("John Doe", saved.getAuthors().get(0).getName());
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DatabaseTest

# Run with debug output
mvn test -X
```

## Debugging

### Enable Debug Logging

Edit `src/main/resources/logback.xml`:

```xml
<logger name="com.homelibrary" level="DEBUG" />
```

### View Logs

```bash
tail -f logs/homelibrary.log
```

### Database Inspection

```bash
sqlite3 homelibrary.db

# Inside SQLite shell:
.tables                    # List tables
.schema book              # Show table schema
SELECT * FROM book;       # Query data
```

## Performance Tips

1. **Use PreparedStatements**: Already implemented
2. **Add Indexes**: Add to `Database.java` for frequently queried columns
3. **Batch Operations**: For bulk inserts
4. **Lazy Loading**: Load related data only when needed

## Code Style Guidelines

- **Naming**: Use camelCase for variables, PascalCase for classes
- **Logging**: Use appropriate levels (DEBUG, INFO, WARN, ERROR)
- **Comments**: Add Javadoc for public methods
- **Error Handling**: Always catch and log exceptions
- **Null Safety**: Use Optional<T> where appropriate

## Building Distribution

### Create Executable JAR

```bash
mvn clean package
# Creates target/home-library-1.0.0.jar
```

### Create Platform Installer (Java 14+)

```bash
jpackage --input target \
         --name HomeLibrary \
         --main-jar home-library-1.0.0.jar \
         --main-class com.homelibrary.ui.MainApp \
         --type dmg  # or exe, deb, rpm
```

## Contributing

When adding features:

1. Follow existing code structure
2. Add appropriate logging
3. Include error handling
4. Update documentation
5. Write tests if applicable
6. Test thoroughly before committing

## Resources

- [JavaFX Documentation](https://openjfx.io/)
- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [Maven Documentation](https://maven.apache.org/guides/)
- [SLF4J Manual](http://www.slf4j.org/manual.html)

## Support

For issues or questions:
1. Check application logs
2. Review existing code
3. Consult this guide
4. Check JavaFX/SQLite documentation
