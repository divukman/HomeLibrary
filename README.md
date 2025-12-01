# Home Library Manager

A comprehensive JavaFX desktop application for managing your personal book collection with SQLite storage and optional Amazon API integration.

<img width="2159" height="1098" alt="image" src="https://github.com/user-attachments/assets/9bf44d85-41e0-4737-801e-5b796294f6df" />


## Features

- **Complete Book Management**: Track title, subtitle, authors, ISBN, publisher, year, category, shelf location, and more
- **Multi-Author Support**: Books can have multiple authors
- **Categories & Tags**: Organize books with categories and custom tags
- **Cover Images**: Upload local cover images or retrieve from Amazon
- **Read Status & Ratings**: Track which books you've read and rate them (0-5 stars)
- **Advanced Search**: Search by title, author, or ISBN
- **Filter Options**: Filter by read status, category, and more
- **Amazon API Integration**: Lookup ISBN and fetch cover images (when configured)
- **SQLite Database**: Lightweight, file-based storage with no server required

## Technology Stack

- **Java 17**: Modern Java features
- **JavaFX 21**: Rich desktop UI
- **SQLite**: Embedded database
- **Maven**: Build and dependency management
- **SLF4J + Logback**: Comprehensive logging
- **OkHttp**: HTTP client for API calls
- **GSON**: JSON processing

## Project Structure

```
HomeLibrary/
├── src/main/java/com/homelibrary/
│   ├── model/          # Data models (Book, Author, Category)
│   ├── dao/            # Database access layer
│   ├── service/        # Business logic layer
│   └── ui/             # JavaFX UI components
├── src/main/resources/ # Configuration files
├── covers/             # Cover image storage (created at runtime)
├── logs/               # Application logs (created at runtime)
├── pom.xml             # Maven configuration
└── README.md           # This file
```

## Prerequisites

- **Java Development Kit (JDK) 17 or higher**
- **Maven 3.6+** (or use Maven wrapper)
- **JavaFX 21** (automatically downloaded by Maven)

## Installation & Setup

### 1. Clone or Extract the Project

```bash
cd HomeLibrary
```

### 2. Build the Project

```bash
mvn clean package
```

This will:
- Download all dependencies
- Compile the code
- Run tests
- Create an executable JAR

### 3. Run the Application

**Option A: Using Launcher Scripts (Recommended)**

For Linux/Mac:
```bash
./run.sh
```

For Windows:
```batch
run.bat
```

The launcher scripts will:
- Check Java installation and version
- Automatically build if needed
- Configure JavaFX module path
- Launch the application

**Option B: Using Maven (Development)**
```bash
mvn javafx:run
```

**Option C: Using Java Directly**

Linux/Mac:
```bash
java --module-path target/lib/javafx-controls-21.0.1.jar:target/lib/javafx-graphics-21.0.1.jar:target/lib/javafx-base-21.0.1.jar:target/lib/javafx-fxml-21.0.1.jar \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/home-library-1.0.0.jar
```

Windows:
```batch
java --module-path target\lib\javafx-controls-21.0.1.jar;target\lib\javafx-graphics-21.0.1.jar;target\lib\javafx-base-21.0.1.jar;target\lib\javafx-fxml-21.0.1.jar ^
     --add-modules javafx.controls,javafx.fxml ^
     -jar target\home-library-1.0.0.jar
```

## First Run

On first run, the application will:
1. Create `homelibrary.db` SQLite database file
2. Initialize the database schema
3. Create `config.properties` configuration file
4. Create `covers/` directory for book cover images
5. Create `logs/` directory for application logs

## Configuration

### Application Settings

The `config.properties` file is created automatically with default values:

```properties
# Amazon API Configuration (optional)
amazon.api.access.key=
amazon.api.secret.key=
amazon.api.associate.tag=
amazon.api.region=us-east-1

# Application Settings
covers.directory=covers
database.file=homelibrary.db
```

### Amazon Product Advertising API (Optional)

To enable Amazon API features:

1. Sign up for [Amazon Product Advertising API](https://affiliate-program.amazon.com/assoc_credentials/home)
2. Get your Access Key, Secret Key, and Associate Tag
3. Edit `config.properties` and add your credentials
4. Restart the application

**Note**: The current Amazon API implementation provides a foundation but requires full AWS Signature Version 4 authentication for production use.

## Usage Guide

### Adding Books

1. Click **"Add Book"** button or use menu: Book → Add Book
2. Fill in book details (only Title is required)
3. Optionally upload a cover image
4. Click **"Save"**

### Editing Books

1. Select a book in the table
2. Double-click or click **"Edit Book"**
3. Modify details
4. Click **"Save"**

### Deleting Books

1. Select a book in the table
2. Click **"Delete Book"**
3. Confirm deletion

### Searching Books

- Use the search box at the top to search by title, author, or ISBN
- Results update as you type
- Clear the search box to show all books

### Filtering Books

- Use the filter dropdown to show:
  - All Books
  - Read Books
  - Unread Books

### Cover Images

**Upload Local Image:**
1. In the book form, click **"Upload Image"**
2. Select an image file (PNG, JPG, JPEG, GIF)
3. Image is automatically copied to the covers directory

**Search Amazon (when configured):**
1. Enter book title (and optionally author)
2. Click **"Search Amazon"**
3. Review results and import data

### Organizing Books

**Categories:**
- Select from existing categories or type a new one
- Categories are created automatically when you save

**Tags:**
- Enter comma-separated tags (e.g., "fiction, sci-fi, classic")

**Shelf Location:**
- Track physical location (e.g., "Living Room A3")

## Database Schema

The SQLite database consists of four main tables:

### `book`
Main book information including title, ISBN, publisher, year, format, language, notes, read status, rating, and cover image path.

### `author`
Author information (ID and name).

### `category`
Book categories (ID and name).

### `book_author`
Many-to-many relationship between books and authors.

## Development

### Project Structure Details

**Model Layer (`model/`)**
- Plain Java objects representing domain entities
- `Book.java`: Complete book information
- `Author.java`: Author details
- `Category.java`: Category details

**DAO Layer (`dao/`)**
- Database access using JDBC
- `Database.java`: Connection management and schema initialization
- `BookDao.java`: CRUD operations for books
- `AuthorDao.java`: CRUD operations for authors
- `CategoryDao.java`: CRUD operations for categories

**Service Layer (`service/`)**
- Business logic and orchestration
- `BookService.java`: Book management operations
- `ConfigService.java`: Configuration management
- `AmazonApiService.java`: Amazon API integration (foundation)

**UI Layer (`ui/`)**
- JavaFX components
- `MainApp.java`: Application entry point and main window
- `BookListView.java`: Book table and search
- `BookFormView.java`: Add/edit book dialog

### Building from Source

```bash
# Clean build
mvn clean

# Compile only
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Skip tests during build
mvn package -DskipTests
```

### IDE Setup

**IntelliJ IDEA:**
1. Open → Select `pom.xml`
2. Enable auto-import for Maven
3. Run configuration: Main class = `com.homelibrary.ui.MainApp`
4. Add VM options: `--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml`

**Eclipse:**
1. File → Import → Maven → Existing Maven Projects
2. Select project directory
3. Run As → Java Application → MainApp

**VS Code:**
1. Install Java Extension Pack
2. Install Maven for Java extension
3. Open folder
4. Run from Run menu

## Logging

Application logs are written to:
- **Console**: INFO level and above
- **File**: `logs/homelibrary.log` (DEBUG level)
- **Rolling**: Daily rotation, keeps 30 days

Log levels can be adjusted in `src/main/resources/logback.xml`.

## Distribution & Sharing

### Sharing the Application

When sharing the application with others who don't have Maven installed, provide:

1. The entire `target/` directory (contains JAR + dependencies)
2. The `run.sh` (Linux/Mac) and/or `run.bat` (Windows) launcher scripts
3. This README file

Users only need Java 17+ installed. They can run the application using the launcher scripts.

### Creating a Release Package

**Easy Method - Automated Script:**

```bash
./create-release.sh
```

This creates:
- `release/home-library-v1.0.0.zip` (for Windows users)
- `release/home-library-v1.0.0.tar.gz` (for Linux/Mac users)

Both archives contain:
- The runnable JAR
- All dependencies
- Launcher scripts (run.sh and run.bat)
- README and installation instructions

**Manual Method:**

```bash
# Build the application
mvn clean package

# Create a distribution archive
cd target
zip -r ../home-library-v1.0.0.zip home-library-1.0.0.jar lib/
cd ..

# Add launcher scripts to the archive
zip home-library-v1.0.0.zip run.sh run.bat README.md
```

**Users can then:**
1. Download and extract the archive
2. Run `./run.sh` (Linux/Mac) or `run.bat` (Windows)
3. No Maven required - only Java 17+!

## Troubleshooting

### JavaFX Not Found

If you see "Error: JavaFX runtime components are missing":

```bash
# Use the launcher script
./run.sh  # or run.bat on Windows

# Or use Maven to run
mvn javafx:run
```

### Database Locked

If database is locked:
1. Close all instances of the application
2. Delete `homelibrary.db-journal` if it exists
3. Restart application

### Cover Images Not Displaying

1. Check that `covers/` directory exists
2. Verify file permissions
3. Check logs for error messages

### Amazon API Not Working

1. Verify credentials in `config.properties`
2. Check internet connection
3. Note: Full PA-API 5.0 implementation requires AWS signing

## Future Enhancements

Potential features for future versions:
- [ ] Barcode scanning via webcam
- [ ] Import from Goodreads/LibraryThing
- [ ] Reading progress tracking
- [ ] JSON/CSV export
- [ ] Cloud sync support
- [ ] Advanced reporting
- [ ] Book lending tracker
- [ ] Full Amazon PA-API 5.0 integration

## License

This project is created for personal use. Feel free to modify and extend as needed.

## Support

For issues or questions:
1. Check the logs in `logs/homelibrary.log`
2. Review the code documentation
3. Examine the specification in `home_library_spec.md`

## Version History

**v1.0.0** - Initial Release
- Complete book management
- SQLite database
- JavaFX UI
- Cover image support
- Search and filter
- Amazon API foundation
