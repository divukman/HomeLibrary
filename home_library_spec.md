# Home Library Software Specification

## Overview
A standalone Java application for managing a home library.  
Uses SQLite for storage and supports book cover retrieval and ISBN lookup from Amazon (via their Product Advertising API).

---

## Features

### Core Book Fields
- **Title**
- **Subtitle** (optional)
- **Authors** (multi-author support)
- **ISBN-10 / ISBN-13**
- **Publisher**
- **Year Published**
- **Category**
- **Shelf / Location**
- **Tags**
- **Format** (Hardcover, Paperback, eBook, PDF)
- **Language**
- **Notes**
- **Date Added**
- **Read Status** (boolean)
- **Rating**
- **Cover Image Path**
- **Amazon ASIN** (optional)

---

## Cover Image Support
Books can include cover images in two ways:

### 1. **Local Image Files**
User can upload book cover images manually.

### 2. **Retrieve from Amazon**  
Using **Amazon Product Advertising API (PA-API 5.0)**:
- Search by title / author
- Fetch ISBNs
- Retrieve cover image URLs
- Download and store images locally

---

## SQLite Schema

```sql
CREATE TABLE category (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE author (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL
);

CREATE TABLE book (
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
  FOREIGN KEY(categoryId) REFERENCES category(id)
);

CREATE TABLE book_author (
  bookId INTEGER,
  authorId INTEGER,
  PRIMARY KEY (bookId, authorId),
  FOREIGN KEY(bookId) REFERENCES book(id),
  FOREIGN KEY(authorId) REFERENCES author(id)
);
```

---

## Amazon API Integration

### Requirements
- Amazon PA-API credentials (Access Key, Secret Key, Associate Tag)

### ISBN Retrieval
- Search endpoint: `SearchItems`
- Extract ISBNs from returned product attributes

### Cover Image Retrieval
- Use `Images.Primary.Large.URL` or fallback size
- Download image and store at:  
  `covers/<bookId>.jpg`

---

## Application Architecture

```
src/
 ├── model/
 │    ├── Book.java
 │    ├── Author.java
 │    └── Category.java
 ├── dao/
 │    ├── Database.java
 │    ├── BookDao.java
 │    ├── AuthorDao.java
 │    └── CategoryDao.java
 ├── service/
 │    ├── BookService.java
 │    └── AmazonApiService.java
 └── ui/
      ├── MainApp.java
      ├── BookListView.java
      └── BookFormView.java
```

---

## JavaFX UI
- TableView for listing all books
- Form for creating/editing books
- Buttons:
  - Add Book
  - Edit Book
  - Delete Book
  - Search Amazon for ISBN / cover
  - Refresh
- Image preview pane for cover display

---

## Future Enhancements
- Barcode scanning from webcam
- Goodreads/LibraryThing import
- Reading progress tracking
- JSON/CSV export and sync

---

*Generated for Claude AI*
