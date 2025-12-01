package com.homelibrary.service;

import com.homelibrary.model.Author;
import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for exporting and importing library data.
 */
public class ExportImportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportImportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookService bookService;

    public ExportImportService(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Export all books to a ZIP file containing JSON and images.
     */
    public void exportToJson(File file) throws Exception {
        logger.info("Exporting books to: {}", file.getAbsolutePath());

        List<Book> books = bookService.getAllBooks();

        // Change extension to .zip if it's .json
        String fileName = file.getAbsolutePath();
        if (fileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.length() - 5) + ".zip";
            file = new File(fileName);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            // Write JSON data
            ZipEntry jsonEntry = new ZipEntry("library.json");
            zos.putNextEntry(jsonEntry);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos));
            writer.write("{\n");
            writer.write("  \"exportDate\": \"" + LocalDateTime.now().format(DATE_FORMATTER) + "\",\n");
            writer.write("  \"version\": \"2.0\",\n");
            writer.write("  \"books\": [\n");

            Set<String> exportedImages = new HashSet<>();

            for (int i = 0; i < books.size(); i++) {
                Book book = books.get(i);
                writer.write("    {\n");
                writeJsonField(writer, "title", book.getTitle(), true);
                writeJsonField(writer, "subtitle", book.getSubtitle(), false);
                writeJsonField(writer, "isbn10", book.getIsbn10(), false);
                writeJsonField(writer, "isbn13", book.getIsbn13(), false);
                writeJsonField(writer, "publisher", book.getPublisher(), false);
                writeJsonField(writer, "yearPublished", book.getYearPublished(), false);
                writeJsonField(writer, "shelfLocation", book.getShelfLocation(), false);
                writeJsonField(writer, "tags", book.getTags(), false);
                writeJsonField(writer, "format", book.getFormat(), false);
                writeJsonField(writer, "language", book.getLanguage(), false);
                writeJsonField(writer, "notes", book.getNotes(), false);
                writeJsonField(writer, "physicalLocation", book.getPhysicalLocation(), false);
                writeJsonField(writer, "amazonAsin", book.getAmazonAsin(), false);

                // Store relative image path for portability
                if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
                    File imageFile = new File(book.getCoverImagePath());
                    if (imageFile.exists()) {
                        String imageName = imageFile.getName();
                        writeJsonField(writer, "coverImagePath", "images/" + imageName, false);
                        exportedImages.add(book.getCoverImagePath());
                    } else {
                        writeJsonField(writer, "coverImagePath", null, false);
                    }
                } else {
                    writeJsonField(writer, "coverImagePath", null, false);
                }

                writeJsonField(writer, "isRead", book.isRead(), false);
                writeJsonField(writer, "isBorrowed", book.isBorrowed(), false);
                writeJsonField(writer, "borrowedTo", book.getBorrowedTo(), false);
                writeJsonField(writer, "rating", book.getRating(), false);

                if (book.getDateAdded() != null) {
                    writeJsonField(writer, "dateAdded", book.getDateAdded().format(DATE_FORMATTER), false);
                }
                if (book.getBorrowedDate() != null) {
                    writeJsonField(writer, "borrowedDate", book.getBorrowedDate().format(DATE_FORMATTER), false);
                }

                // Category
                if (book.getCategory() != null) {
                    writer.write("      \"category\": \"" + escapeJson(book.getCategory().getName()) + "\",\n");
                }

                // Authors
                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    writer.write("      \"authors\": [\n");
                    for (int j = 0; j < book.getAuthors().size(); j++) {
                        Author author = book.getAuthors().get(j);
                        writer.write("        \"" + escapeJson(author.getName()) + "\"");
                        if (j < book.getAuthors().size() - 1) {
                            writer.write(",");
                        }
                        writer.write("\n");
                    }
                    writer.write("      ]\n");
                } else {
                    writer.write("      \"authors\": []\n");
                }

                writer.write("    }");
                if (i < books.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  ]\n");
            writer.write("}\n");
            writer.flush();
            zos.closeEntry();

            // Export images
            int imageCount = 0;
            for (String imagePath : exportedImages) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    try {
                        ZipEntry imageEntry = new ZipEntry("images/" + imageFile.getName());
                        zos.putNextEntry(imageEntry);
                        Files.copy(imageFile.toPath(), zos);
                        zos.closeEntry();
                        imageCount++;
                    } catch (Exception e) {
                        logger.warn("Failed to export image: {}", imagePath, e);
                    }
                }
            }

            logger.info("Successfully exported {} books and {} images", books.size(), imageCount);
        }
    }

    /**
     * Import books from a JSON or ZIP file.
     */
    public ImportResult importFromJson(File file) throws Exception {
        logger.info("Importing books from: {}", file.getAbsolutePath());

        String content;
        File tempImagesDir = null;

        // Check if file is a ZIP
        if (file.getName().endsWith(".zip")) {
            // Extract ZIP to temp directory
            tempImagesDir = Files.createTempDirectory("homelibrary_import").toFile();
            logger.info("Extracting ZIP to: {}", tempImagesDir.getAbsolutePath());

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                ZipEntry entry;
                byte[] buffer = new byte[8192];

                while ((entry = zis.getNextEntry()) != null) {
                    File entryFile = new File(tempImagesDir, entry.getName());

                    if (entry.isDirectory()) {
                        entryFile.mkdirs();
                    } else {
                        entryFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            // Read JSON from extracted files
            File jsonFile = new File(tempImagesDir, "library.json");
            if (!jsonFile.exists()) {
                throw new Exception("Invalid ZIP file: library.json not found");
            }
            content = Files.readString(jsonFile.toPath());
        } else {
            // Read plain JSON file
            content = Files.readString(file.toPath());
        }

        List<Book> books = parseJsonBooks(content);

        // Get existing books to check for duplicates
        List<Book> existingBooks = bookService.getAllBooks();
        Set<String> existingIsbn10 = new HashSet<>();
        Set<String> existingIsbn13 = new HashSet<>();
        Map<String, Book> existingTitles = new HashMap<>();

        for (Book existing : existingBooks) {
            if (existing.getIsbn10() != null && !existing.getIsbn10().isEmpty()) {
                existingIsbn10.add(existing.getIsbn10());
            }
            if (existing.getIsbn13() != null && !existing.getIsbn13().isEmpty()) {
                existingIsbn13.add(existing.getIsbn13());
            }
            if (existing.getTitle() != null) {
                existingTitles.put(existing.getTitle().toLowerCase(), existing);
            }
        }

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        // Create permanent images directory if needed
        File imagesDir = new File("data/images");
        imagesDir.mkdirs();

        for (Book book : books) {
            try {
                // Check for duplicates
                boolean isDuplicate = false;
                String duplicateReason = "";

                if (book.getIsbn10() != null && !book.getIsbn10().isEmpty() && existingIsbn10.contains(book.getIsbn10())) {
                    isDuplicate = true;
                    duplicateReason = "ISBN-10: " + book.getIsbn10();
                } else if (book.getIsbn13() != null && !book.getIsbn13().isEmpty() && existingIsbn13.contains(book.getIsbn13())) {
                    isDuplicate = true;
                    duplicateReason = "ISBN-13: " + book.getIsbn13();
                } else if (book.getTitle() != null && existingTitles.containsKey(book.getTitle().toLowerCase())) {
                    Book existing = existingTitles.get(book.getTitle().toLowerCase());
                    // Check if authors also match
                    if (authorsMatch(book.getAuthors(), existing.getAuthors())) {
                        isDuplicate = true;
                        duplicateReason = "Title and authors match";
                    }
                }

                if (isDuplicate) {
                    skipCount++;
                    skipped.add("Skipped '" + book.getTitle() + "' (" + duplicateReason + ")");
                    logger.info("Skipping duplicate book: {} - {}", book.getTitle(), duplicateReason);
                    continue;
                }

                // Handle image import
                if (tempImagesDir != null && book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
                    String imagePath = book.getCoverImagePath();
                    File sourceImage = new File(tempImagesDir, imagePath);

                    if (sourceImage.exists()) {
                        String imageName = sourceImage.getName();
                        File targetImage = new File(imagesDir, imageName);

                        // Copy image to permanent location
                        Files.copy(sourceImage.toPath(), targetImage.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        book.setCoverImagePath(targetImage.getAbsolutePath());
                        logger.info("Imported image: {}", imageName);
                    } else {
                        logger.warn("Image not found in ZIP: {}", imagePath);
                        book.setCoverImagePath(null);
                    }
                } else if (book.getCoverImagePath() != null) {
                    // Clear non-existent image path
                    book.setCoverImagePath(null);
                }

                // Clear ID to create new books
                book.setId(null);

                // Ensure authors list is not null
                if (book.getAuthors() == null) {
                    book.setAuthors(new ArrayList<>());
                }

                bookService.saveBook(book);
                successCount++;
                logger.info("Imported book: {}", book.getTitle());
            } catch (Exception e) {
                errorCount++;
                String errorMsg = "Failed to import book '" + (book.getTitle() != null ? book.getTitle() : "unknown") + "': " + e.getMessage();
                errors.add(errorMsg);
                logger.error(errorMsg, e);
            }
        }

        // Clean up temp directory
        if (tempImagesDir != null) {
            deleteDirectory(tempImagesDir);
        }

        logger.info("Import completed: {} successful, {} duplicates skipped, {} errors", successCount, skipCount, errorCount);
        return new ImportResult(successCount, errorCount, errors, skipCount, skipped);
    }

    /**
     * Check if two author lists match.
     */
    private boolean authorsMatch(List<Author> authors1, List<Author> authors2) {
        if (authors1 == null || authors1.isEmpty()) {
            return authors2 == null || authors2.isEmpty();
        }
        if (authors2 == null || authors2.isEmpty()) {
            return false;
        }

        Set<String> names1 = new HashSet<>();
        Set<String> names2 = new HashSet<>();

        for (Author a : authors1) {
            if (a != null && a.getName() != null) {
                names1.add(a.getName().toLowerCase());
            }
        }
        for (Author a : authors2) {
            if (a != null && a.getName() != null) {
                names2.add(a.getName().toLowerCase());
            }
        }

        return names1.equals(names2);
    }

    /**
     * Delete directory recursively.
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * Parse JSON content and extract books.
     */
    private List<Book> parseJsonBooks(String json) throws Exception {
        List<Book> books = new ArrayList<>();

        // Find books array
        int booksStart = json.indexOf("\"books\": [");
        if (booksStart == -1) {
            throw new Exception("Invalid JSON format: 'books' array not found");
        }

        int arrayStart = json.indexOf('[', booksStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);

        String booksJson = json.substring(arrayStart + 1, arrayEnd);

        // Parse each book object
        int pos = 0;
        while (pos < booksJson.length()) {
            int objStart = booksJson.indexOf('{', pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBrace(booksJson, objStart);
            String bookJson = booksJson.substring(objStart + 1, objEnd);

            Book book = parseBook(bookJson);
            books.add(book);

            pos = objEnd + 1;
        }

        return books;
    }

    /**
     * Parse a single book from JSON string.
     */
    private Book parseBook(String json) throws Exception {
        Book book = new Book();

        book.setTitle(extractStringValue(json, "title"));
        book.setSubtitle(extractStringValue(json, "subtitle"));
        book.setIsbn10(extractStringValue(json, "isbn10"));
        book.setIsbn13(extractStringValue(json, "isbn13"));
        book.setPublisher(extractStringValue(json, "publisher"));
        book.setShelfLocation(extractStringValue(json, "shelfLocation"));
        book.setTags(extractStringValue(json, "tags"));
        book.setFormat(extractStringValue(json, "format"));
        book.setLanguage(extractStringValue(json, "language"));
        book.setNotes(extractStringValue(json, "notes"));
        book.setPhysicalLocation(extractStringValue(json, "physicalLocation"));
        book.setAmazonAsin(extractStringValue(json, "amazonAsin"));
        book.setCoverImagePath(extractStringValue(json, "coverImagePath"));
        book.setBorrowedTo(extractStringValue(json, "borrowedTo"));

        String yearStr = extractStringValue(json, "yearPublished");
        if (yearStr != null && !yearStr.isEmpty()) {
            book.setYearPublished(Integer.parseInt(yearStr));
        }

        String ratingStr = extractStringValue(json, "rating");
        if (ratingStr != null && !ratingStr.isEmpty()) {
            book.setRating(Integer.parseInt(ratingStr));
        }

        String isReadStr = extractStringValue(json, "isRead");
        book.setRead("true".equals(isReadStr));

        String isBorrowedStr = extractStringValue(json, "isBorrowed");
        book.setBorrowed("true".equals(isBorrowedStr));

        String dateAddedStr = extractStringValue(json, "dateAdded");
        if (dateAddedStr != null && !dateAddedStr.isEmpty()) {
            book.setDateAdded(LocalDateTime.parse(dateAddedStr, DATE_FORMATTER));
        }

        String borrowedDateStr = extractStringValue(json, "borrowedDate");
        if (borrowedDateStr != null && !borrowedDateStr.isEmpty()) {
            book.setBorrowedDate(LocalDateTime.parse(borrowedDateStr, DATE_FORMATTER));
        }

        // Parse category
        String categoryName = extractStringValue(json, "category");
        if (categoryName != null && !categoryName.isEmpty()) {
            Category category = new Category();
            category.setName(categoryName);
            book.setCategory(category);
        }

        // Parse authors
        List<String> authorNames = extractStringArray(json, "authors");
        for (String authorName : authorNames) {
            Author author = new Author();
            author.setName(authorName);
            book.getAuthors().add(author);
        }

        return book;
    }

    /**
     * Extract string value from JSON.
     */
    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyPos = json.indexOf(pattern);
        if (keyPos == -1) return null;

        // Find the colon after the key
        int colonPos = json.indexOf(':', keyPos + pattern.length());
        if (colonPos == -1) return null;

        // Find the opening quote after the colon
        int start = -1;
        for (int i = colonPos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                start = i + 1;
                break;
            } else if (!Character.isWhitespace(c)) {
                // Non-quote, non-whitespace character found (e.g., null, number, boolean)
                return null;
            }
        }

        if (start == -1) return null;

        // Find the closing quote
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && (end == start || json.charAt(end - 1) != '\\')) {
                break;
            }
            end++;
        }

        if (end >= json.length()) return null;

        String value = json.substring(start, end);
        return unescapeJson(value);
    }

    /**
     * Extract string array from JSON.
     */
    private List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();

        String pattern = "\"" + key + "\"\\s*:\\s*\\[";
        int start = json.indexOf(pattern);
        if (start == -1) return result;

        int arrayStart = json.indexOf('[', start);
        int arrayEnd = findMatchingBracket(json, arrayStart);

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);

        int pos = 0;
        while (pos < arrayContent.length()) {
            int strStart = arrayContent.indexOf('"', pos);
            if (strStart == -1) break;

            int strEnd = strStart + 1;
            while (strEnd < arrayContent.length()) {
                if (arrayContent.charAt(strEnd) == '"' && arrayContent.charAt(strEnd - 1) != '\\') {
                    break;
                }
                strEnd++;
            }

            if (strEnd >= arrayContent.length()) break;

            String value = arrayContent.substring(strStart + 1, strEnd);
            result.add(unescapeJson(value));

            pos = strEnd + 1;
        }

        return result;
    }

    /**
     * Find matching closing bracket.
     */
    private int findMatchingBracket(String str, int start) {
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            if (str.charAt(i) == '[') count++;
            else if (str.charAt(i) == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    /**
     * Find matching closing brace.
     */
    private int findMatchingBrace(String str, int start) {
        int count = 1;
        boolean inString = false;

        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '"' && str.charAt(i - 1) != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') count++;
                else if (c == '}') {
                    count--;
                    if (count == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * Write a JSON field.
     */
    private void writeJsonField(BufferedWriter writer, String key, Object value, boolean isFirst) throws IOException {
        if (value == null) return;

        String valueStr = value instanceof String ? escapeJson((String) value) : value.toString();
        writer.write("      \"" + key + "\": ");

        if (value instanceof String) {
            writer.write("\"" + valueStr + "\"");
        } else if (value instanceof Boolean || value instanceof Number) {
            writer.write(valueStr);
        } else {
            writer.write("\"" + valueStr + "\"");
        }

        writer.write(",\n");
    }

    /**
     * Escape special characters for JSON.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Unescape JSON special characters.
     */
    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }

    /**
     * Result of an import operation.
     */
    public static class ImportResult {
        private final int successCount;
        private final int errorCount;
        private final List<String> errors;
        private final int skipCount;
        private final List<String> skipped;

        public ImportResult(int successCount, int errorCount, List<String> errors) {
            this(successCount, errorCount, errors, 0, new ArrayList<>());
        }

        public ImportResult(int successCount, int errorCount, List<String> errors, int skipCount, List<String> skipped) {
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.skipCount = skipCount;
            this.skipped = skipped != null ? skipped : new ArrayList<>();
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getSkipCount() {
            return skipCount;
        }

        public List<String> getSkipped() {
            return skipped;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasSkipped() {
            return skipCount > 0;
        }
    }
}
