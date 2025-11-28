package com.homelibrary.ui;

import com.homelibrary.model.Author;
import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import com.homelibrary.service.AmazonApiService;
import com.homelibrary.service.BookService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Dialog for creating/editing books.
 */
public class BookFormView extends Dialog<Book> {
    private static final Logger logger = LoggerFactory.getLogger(BookFormView.class);

    private final MainApp mainApp;
    private final BookService bookService;
    private final AmazonApiService amazonService;
    private final Book book;
    private final boolean isNewBook;

    // Form fields
    private TextField titleField;
    private TextField subtitleField;
    private TextField authorsField;
    private TextField isbn10Field;
    private TextField isbn13Field;
    private TextField publisherField;
    private TextField yearField;
    private ComboBox<Category> categoryCombo;
    private TextField shelfLocationField;
    private TextField tagsField;
    private ComboBox<String> formatCombo;
    private TextField languageField;
    private TextArea notesArea;
    private CheckBox isReadCheck;
    private Spinner<Integer> ratingSpinner;
    private ImageView coverImageView;
    private String coverImagePath;

    public BookFormView(MainApp mainApp, Book book) {
        this.mainApp = mainApp;
        this.bookService = new BookService();
        this.amazonService = new AmazonApiService();
        this.book = book != null ? book : new Book();
        this.isNewBook = book == null;

        initDialog();
        createForm();
        if (!isNewBook) {
            populateFields();
        }
    }

    /**
     * Initialize dialog properties.
     */
    private void initDialog() {
        setTitle(isNewBook ? "Add New Book" : "Edit Book");
        setHeaderText(isNewBook ? "Enter book details" : "Edit book details");

        // Set dialog as modal
        initModality(Modality.APPLICATION_MODAL);
        initOwner(mainApp.getPrimaryStage());

        // Add OK and Cancel buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        logger.debug("Dialog buttons added: Save and Cancel");
        logger.debug("Button types count: {}", getDialogPane().getButtonTypes().size());

        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                logger.debug("Save button clicked, calling saveBook()");
                return saveBook();
            }
            return null;
        });
    }

    /**
     * Create the form layout.
     */
    private void createForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Title
        grid.add(new Label("Title:"), 0, row);
        titleField = new TextField();
        titleField.setPromptText("Book title");
        grid.add(titleField, 1, row);
        row++;

        // Subtitle
        grid.add(new Label("Subtitle:"), 0, row);
        subtitleField = new TextField();
        subtitleField.setPromptText("Subtitle (optional)");
        grid.add(subtitleField, 1, row);
        row++;

        // Authors
        grid.add(new Label("Authors:"), 0, row);
        authorsField = new TextField();
        authorsField.setPromptText("Comma-separated author names");
        grid.add(authorsField, 1, row);
        row++;

        // ISBN-10
        grid.add(new Label("ISBN-10:"), 0, row);
        isbn10Field = new TextField();
        isbn10Field.setPromptText("10-digit ISBN");
        grid.add(isbn10Field, 1, row);
        row++;

        // ISBN-13
        grid.add(new Label("ISBN-13:"), 0, row);
        isbn13Field = new TextField();
        isbn13Field.setPromptText("13-digit ISBN");
        grid.add(isbn13Field, 1, row);
        row++;

        // Publisher
        grid.add(new Label("Publisher:"), 0, row);
        publisherField = new TextField();
        publisherField.setPromptText("Publisher name");
        grid.add(publisherField, 1, row);
        row++;

        // Year Published
        grid.add(new Label("Year:"), 0, row);
        yearField = new TextField();
        yearField.setPromptText("Publication year");
        grid.add(yearField, 1, row);
        row++;

        // Category
        grid.add(new Label("Category:"), 0, row);
        categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        categoryCombo.setPromptText("Select or enter category");
        loadCategories();
        grid.add(categoryCombo, 1, row);
        row++;

        // Shelf Location
        grid.add(new Label("Shelf:"), 0, row);
        shelfLocationField = new TextField();
        shelfLocationField.setPromptText("Shelf location");
        grid.add(shelfLocationField, 1, row);
        row++;

        // Tags
        grid.add(new Label("Tags:"), 0, row);
        tagsField = new TextField();
        tagsField.setPromptText("Comma-separated tags");
        grid.add(tagsField, 1, row);
        row++;

        // Format
        grid.add(new Label("Format:"), 0, row);
        formatCombo = new ComboBox<>();
        formatCombo.setItems(FXCollections.observableArrayList(
            "Hardcover", "Paperback", "eBook", "PDF", "Audiobook"
        ));
        formatCombo.setEditable(true);
        formatCombo.setPromptText("Select format");
        grid.add(formatCombo, 1, row);
        row++;

        // Language
        grid.add(new Label("Language:"), 0, row);
        languageField = new TextField();
        languageField.setPromptText("Language");
        languageField.setText("English");
        grid.add(languageField, 1, row);
        row++;

        // Read status
        grid.add(new Label("Read:"), 0, row);
        isReadCheck = new CheckBox("I have read this book");
        grid.add(isReadCheck, 1, row);
        row++;

        // Rating
        grid.add(new Label("Rating:"), 0, row);
        ratingSpinner = new Spinner<>(0, 5, 0);
        ratingSpinner.setEditable(true);
        HBox ratingBox = new HBox(5, ratingSpinner, new Label("(0-5 stars)"));
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(ratingBox, 1, row);
        row++;

        // Notes
        grid.add(new Label("Notes:"), 0, row);
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Personal notes");
        grid.add(notesArea, 1, row);
        row++;

        // Cover image section
        VBox coverSection = createCoverSection();
        grid.add(new Label("Cover:"), 0, row);
        grid.add(coverSection, 1, row);

        // Set column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(400);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Create scroll pane for form
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);

        getDialogPane().setContent(scrollPane);
        getDialogPane().setPrefWidth(600);
    }

    /**
     * Create cover image section.
     */
    private VBox createCoverSection() {
        VBox coverBox = new VBox(10);

        coverImageView = new ImageView();
        coverImageView.setFitWidth(150);
        coverImageView.setFitHeight(200);
        coverImageView.setPreserveRatio(true);

        Button uploadButton = new Button("Upload Image");
        uploadButton.setOnAction(e -> handleUploadCover());

        Button searchIsbnButton = new Button("Search by ISBN");
        searchIsbnButton.setOnAction(e -> handleSearchByIsbn());

        Button searchTitleButton = new Button("Search by Title");
        searchTitleButton.setOnAction(e -> handleSearchByTitle());

        HBox buttonBox = new HBox(5, uploadButton, searchIsbnButton, searchTitleButton);

        coverBox.getChildren().addAll(coverImageView, buttonBox);

        return coverBox;
    }

    /**
     * Load categories into combo box.
     */
    private void loadCategories() {
        try {
            List<Category> categories = bookService.getAllCategories();
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            logger.error("Failed to load categories", e);
        }
    }

    /**
     * Populate form fields with book data.
     */
    private void populateFields() {
        titleField.setText(book.getTitle());
        subtitleField.setText(book.getSubtitle());

        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            String authorsStr = book.getAuthors().stream()
                    .map(Author::getName)
                    .collect(Collectors.joining(", "));
            authorsField.setText(authorsStr);
        }

        isbn10Field.setText(book.getIsbn10());
        isbn13Field.setText(book.getIsbn13());
        publisherField.setText(book.getPublisher());

        if (book.getYearPublished() != null) {
            yearField.setText(book.getYearPublished().toString());
        }

        if (book.getCategory() != null) {
            categoryCombo.setValue(book.getCategory());
        }

        shelfLocationField.setText(book.getShelfLocation());
        tagsField.setText(book.getTags());
        formatCombo.setValue(book.getFormat());
        languageField.setText(book.getLanguage());
        notesArea.setText(book.getNotes());
        isReadCheck.setSelected(book.isRead());

        if (book.getRating() != null) {
            ratingSpinner.getValueFactory().setValue(book.getRating());
        }

        // Load cover image
        if (book.getCoverImagePath() != null) {
            coverImagePath = book.getCoverImagePath();
            loadCoverImage(coverImagePath);
        }
    }

    /**
     * Load cover image into preview.
     */
    private void loadCoverImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                coverImageView.setImage(image);
            }
        } catch (Exception e) {
            logger.error("Failed to load cover image", e);
        }
    }

    /**
     * Handle upload cover button.
     */
    private void handleUploadCover() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cover Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Preview image
                Image image = new Image(selectedFile.toURI().toString());
                coverImageView.setImage(image);
                coverImagePath = selectedFile.getAbsolutePath();
            } catch (Exception e) {
                logger.error("Failed to load image", e);
                mainApp.showErrorAlert("Error", "Failed to load image: " + e.getMessage());
            }
        }
    }

    /**
     * Handle search by ISBN button.
     */
    private void handleSearchByIsbn() {
        String isbn = isbn13Field.getText().trim();
        if (isbn.isEmpty()) {
            isbn = isbn10Field.getText().trim();
        }

        if (isbn.isEmpty()) {
            mainApp.showInfoAlert("Missing Information", "Please enter an ISBN first.");
            return;
        }

        mainApp.showInfoAlert("ISBN Search",
            "ISBN search feature will look up book information using:\n\n" +
            "- Google Books API\n" +
            "- Open Library API\n" +
            "- ISBN DB\n\n" +
            "This feature requires API integration.\n" +
            "For now, please enter book details manually or use 'Search by Title'.");
    }

    /**
     * Handle search by title button.
     */
    private void handleSearchByTitle() {
        String title = titleField.getText().trim();

        if (title.isEmpty()) {
            mainApp.showInfoAlert("Missing Information", "Please enter a book title first.");
            return;
        }

        mainApp.showInfoAlert("Title Search",
            "Title search feature will look up book information using:\n\n" +
            "- Google Books API\n" +
            "- Open Library API\n" +
            "- WorldCat\n\n" +
            "This feature requires API integration.\n" +
            "For now, please enter book details manually.");
    }

    /**
     * Save book from form data.
     */
    private Book saveBook() {
        try {
            // Update book object
            book.setTitle(titleField.getText().trim());
            book.setSubtitle(subtitleField.getText().trim());

            // Parse authors
            String authorsText = authorsField.getText().trim();
            if (!authorsText.isEmpty()) {
                List<Author> authors = Arrays.stream(authorsText.split(","))
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .map(Author::new)
                        .collect(Collectors.toList());
                book.setAuthors(authors);
            }

            book.setIsbn10(isbn10Field.getText().trim());
            book.setIsbn13(isbn13Field.getText().trim());
            book.setPublisher(publisherField.getText().trim());

            // Parse year
            String yearText = yearField.getText().trim();
            if (!yearText.isEmpty()) {
                try {
                    book.setYearPublished(Integer.parseInt(yearText));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid year format: {}", yearText);
                }
            }

            // Category
            Object categoryValue = categoryCombo.getValue();
            if (categoryValue instanceof Category) {
                book.setCategory((Category) categoryValue);
            } else if (categoryValue instanceof String && !((String) categoryValue).trim().isEmpty()) {
                // User typed a new category name
                Category newCategory = new Category(((String) categoryValue).trim());
                book.setCategory(newCategory);
            } else if (categoryCombo.getEditor().getText() != null &&
                       !categoryCombo.getEditor().getText().trim().isEmpty()) {
                // Fallback to editor text
                Category newCategory = new Category(categoryCombo.getEditor().getText().trim());
                book.setCategory(newCategory);
            }

            book.setShelfLocation(shelfLocationField.getText().trim());
            book.setTags(tagsField.getText().trim());
            book.setFormat(formatCombo.getValue());
            book.setLanguage(languageField.getText().trim());
            book.setNotes(notesArea.getText().trim());
            book.setRead(isReadCheck.isSelected());
            book.setRating(ratingSpinner.getValue() > 0 ? ratingSpinner.getValue() : null);

            // Save book to get ID
            Book savedBook = bookService.saveBook(book);

            // Handle cover image upload
            if (coverImagePath != null && !coverImagePath.equals(book.getCoverImagePath())) {
                File coverFile = new File(coverImagePath);
                if (coverFile.exists()) {
                    String uploadedPath = bookService.uploadCoverImage(coverFile, savedBook.getId());
                    savedBook.setCoverImagePath(uploadedPath);
                    // Save again with cover path
                    savedBook = bookService.saveBook(savedBook);
                }
            }

            logger.info("Saved book: {}", savedBook);
            return savedBook;

        } catch (Exception e) {
            logger.error("Failed to save book", e);
            // Don't show alert here - it causes modal dialog issues
            // The calling code will handle showing the error
            return null;
        }
    }
}
