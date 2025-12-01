package com.homelibrary.ui;

import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import com.homelibrary.service.BookService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * View for displaying list of books in a table.
 */
public class BookListView {
    private static final Logger logger = LoggerFactory.getLogger(BookListView.class);

    private final MainApp mainApp;
    private final BookService bookService;
    private final BorderPane view;
    private final TableView<Book> bookTable;
    private final ObservableList<Book> bookData;
    private final ImageView coverImageView;
    private final Label statsLabel;
    private ComboBox<Category> categoryFilter;

    // Table columns for visibility control
    private TableColumn<Book, Integer> idCol;
    private TableColumn<Book, String> titleCol;
    private TableColumn<Book, String> authorsCol;
    private TableColumn<Book, String> isbnCol;
    private TableColumn<Book, String> publisherCol;
    private TableColumn<Book, Integer> yearCol;
    private TableColumn<Book, String> categoryCol;
    private TableColumn<Book, String> shelfCol;
    private TableColumn<Book, String> tagsCol;
    private TableColumn<Book, String> formatCol;
    private TableColumn<Book, String> readCol;
    private TableColumn<Book, Integer> ratingCol;
    private TableColumn<Book, String> physicalLocationCol;
    private TableColumn<Book, String> borrowedStatusCol;
    private TableColumn<Book, String> borrowedToCol;

    public BookListView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.bookService = new BookService();
        this.bookData = FXCollections.observableArrayList();
        this.bookTable = new TableView<>();
        this.coverImageView = new ImageView();
        this.statsLabel = new Label();

        this.view = createView();
        loadBooks();
        updateStats();
    }

    /**
     * Create the main view layout.
     */
    private BorderPane createView() {
        BorderPane borderPane = new BorderPane();

        // Top: Search and filter controls
        HBox topBar = createTopBar();
        borderPane.setTop(topBar);

        // Center: Book table
        VBox tableContainer = createTableView();
        borderPane.setCenter(tableContainer);

        // Right: Cover image preview and details
        VBox rightPanel = createRightPanel();
        borderPane.setRight(rightPanel);

        // Bottom: Statistics
        HBox bottomBar = createBottomBar();
        borderPane.setBottom(bottomBar);

        return borderPane;
    }

    /**
     * Create top toolbar with search and buttons.
     */
    private HBox createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search by title, author, ISBN, category, location, borrower...");
        searchField.setPrefWidth(350);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                loadBooks();
            } else {
                searchBooks(newValue.trim());
            }
        });

        // Buttons
        Button addButton = new Button("Add Book");
        addButton.setOnAction(e -> handleAddBook());

        Button editButton = new Button("Edit Book");
        editButton.setOnAction(e -> handleEditBook());

        Button deleteButton = new Button("Delete Book");
        deleteButton.setOnAction(e -> handleDeleteBook());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshBooks());

        Button columnsButton = new Button("Columns");
        columnsButton.setOnAction(e -> showColumnCustomization());

        // Filter by category
        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        loadCategoryFilter(categoryFilter);
        categoryFilter.setOnAction(e -> {
            Category selected = categoryFilter.getValue();
            filterByCategory(selected);
        });

        // Filter by read status
        ComboBox<String> readStatusFilter = new ComboBox<>();
        readStatusFilter.getItems().addAll("All Books", "Read", "Unread");
        readStatusFilter.setValue("All Books");
        readStatusFilter.setOnAction(e -> {
            String selected = readStatusFilter.getValue();
            filterByReadStatus(selected);
        });

        // Filter by borrowed status
        ComboBox<String> borrowedStatusFilter = new ComboBox<>();
        borrowedStatusFilter.getItems().addAll("All", "Borrowed", "Available");
        borrowedStatusFilter.setValue("All");
        borrowedStatusFilter.setOnAction(e -> {
            String selected = borrowedStatusFilter.getValue();
            filterByBorrowedStatus(selected);
        });

        topBar.getChildren().addAll(
            searchField,
            new Separator(),
            addButton,
            editButton,
            deleteButton,
            refreshButton,
            columnsButton,
            new Separator(),
            new Label("Category:"),
            categoryFilter,
            new Label("Read:"),
            readStatusFilter,
            new Label("Status:"),
            borrowedStatusFilter
        );

        return topBar;
    }

    /**
     * Create table view for books.
     */
    private VBox createTableView() {
        VBox container = new VBox();
        container.setPadding(new Insets(10));

        // Configure table
        bookTable.setItems(bookData);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ID column
        idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        // Title column
        titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFullTitle()));
        titleCol.setPrefWidth(250);

        // Authors column
        authorsCol = new TableColumn<>("Authors");
        authorsCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAuthorsString()));
        authorsCol.setPrefWidth(200);

        // ISBN column
        isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            String isbn = book.getIsbn13() != null ? book.getIsbn13() : book.getIsbn10();
            return new SimpleStringProperty(isbn != null ? isbn : "");
        });
        isbnCol.setPrefWidth(120);

        // Publisher column
        publisherCol = new TableColumn<>("Publisher");
        publisherCol.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        publisherCol.setPrefWidth(150);

        // Year column
        yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("yearPublished"));
        yearCol.setPrefWidth(70);

        // Category column
        categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            return new SimpleStringProperty(book.getCategory() != null ? book.getCategory().getName() : "");
        });
        categoryCol.setPrefWidth(120);

        // Read status column
        readCol = new TableColumn<>("Read");
        readCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isRead() ? "Yes" : "No"));
        readCol.setPrefWidth(60);

        // Rating column
        ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ratingCol.setPrefWidth(70);

        // Shelf location column
        shelfCol = new TableColumn<>("Shelf");
        shelfCol.setCellValueFactory(new PropertyValueFactory<>("shelfLocation"));
        shelfCol.setPrefWidth(80);

        // Tags column
        tagsCol = new TableColumn<>("Tags");
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        tagsCol.setPrefWidth(150);

        // Format column
        formatCol = new TableColumn<>("Format");
        formatCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        formatCol.setPrefWidth(90);

        // Physical Location column
        physicalLocationCol = new TableColumn<>("Location");
        physicalLocationCol.setCellValueFactory(new PropertyValueFactory<>("physicalLocation"));
        physicalLocationCol.setPrefWidth(120);

        // Borrowed Status column
        borrowedStatusCol = new TableColumn<>("Borrowed");
        borrowedStatusCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isBorrowed() ? "Yes" : "No"));
        borrowedStatusCol.setPrefWidth(80);

        // Borrowed To column
        borrowedToCol = new TableColumn<>("Borrowed To");
        borrowedToCol.setCellValueFactory(new PropertyValueFactory<>("borrowedTo"));
        borrowedToCol.setPrefWidth(150);

        bookTable.getColumns().addAll(
            idCol, titleCol, authorsCol, isbnCol, publisherCol,
            yearCol, categoryCol, shelfCol, physicalLocationCol, borrowedStatusCol, borrowedToCol,
            tagsCol, formatCol, readCol, ratingCol
        );

        // Selection listener to update cover preview
        bookTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateCoverPreview(newValue)
        );

        // Double-click to edit
        bookTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleEditBook();
            }
        });

        container.getChildren().add(bookTable);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        return container;
    }

    /**
     * Create right panel with cover preview.
     */
    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(250);
        panel.setAlignment(Pos.TOP_CENTER);

        Label coverLabel = new Label("Cover Preview");
        coverLabel.setStyle("-fx-font-weight: bold;");

        coverImageView.setFitWidth(200);
        coverImageView.setFitHeight(300);
        coverImageView.setPreserveRatio(true);

        panel.getChildren().addAll(coverLabel, coverImageView);

        return panel;
    }

    /**
     * Create bottom status bar.
     */
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(5, 10, 5, 10));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setStyle("-fx-background-color: #f0f0f0;");

        statsLabel.setText("Total books: 0");

        bottomBar.getChildren().add(statsLabel);

        return bottomBar;
    }

    /**
     * Load all books from database.
     */
    private void loadBooks() {
        try {
            List<Book> books = bookService.getAllBooks();
            bookData.setAll(books);
            logger.info("Loaded {} books", books.size());
        } catch (SQLException e) {
            logger.error("Failed to load books", e);
            mainApp.showErrorAlert("Error", "Failed to load books: " + e.getMessage());
        }
    }

    /**
     * Search books.
     */
    private void searchBooks(String query) {
        try {
            List<Book> books = bookService.searchBooks(query);
            bookData.setAll(books);
            logger.info("Found {} books matching '{}'", books.size(), query);
        } catch (SQLException e) {
            logger.error("Failed to search books", e);
            mainApp.showErrorAlert("Error", "Failed to search books: " + e.getMessage());
        }
    }

    /**
     * Filter books by read status.
     */
    private void filterByReadStatus(String status) {
        try {
            List<Book> books;
            if ("Read".equals(status)) {
                books = bookService.getBooksByReadStatus(true);
            } else if ("Unread".equals(status)) {
                books = bookService.getBooksByReadStatus(false);
            } else {
                books = bookService.getAllBooks();
            }
            bookData.setAll(books);
        } catch (SQLException e) {
            logger.error("Failed to filter books", e);
            mainApp.showErrorAlert("Error", "Failed to filter books: " + e.getMessage());
        }
    }

    /**
     * Filter books by borrowed status.
     */
    private void filterByBorrowedStatus(String status) {
        try {
            List<Book> books;
            if ("Borrowed".equals(status)) {
                books = bookService.getBooksByBorrowedStatus(true);
            } else if ("Available".equals(status)) {
                books = bookService.getBooksByBorrowedStatus(false);
            } else {
                books = bookService.getAllBooks();
            }
            bookData.setAll(books);
        } catch (SQLException e) {
            logger.error("Failed to filter books by borrowed status", e);
            mainApp.showErrorAlert("Error", "Failed to filter books: " + e.getMessage());
        }
    }

    /**
     * Load categories into the filter dropdown.
     */
    private void loadCategoryFilter(ComboBox<Category> categoryFilter) {
        try {
            Category currentSelection = categoryFilter.getValue();
            List<Category> categories = bookService.getAllCategories();
            categoryFilter.getItems().clear();
            categoryFilter.getItems().addAll(categories);

            // Restore selection if it still exists
            if (currentSelection != null) {
                for (Category cat : categories) {
                    if (cat.getId().equals(currentSelection.getId())) {
                        categoryFilter.setValue(cat);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load categories for filter", e);
        }
    }

    /**
     * Filter books by category.
     */
    private void filterByCategory(Category category) {
        try {
            List<Book> books;
            if (category != null) {
                books = bookService.getBooksByCategory(category.getId());
            } else {
                books = bookService.getAllBooks();
            }
            bookData.setAll(books);
        } catch (SQLException e) {
            logger.error("Failed to filter books by category", e);
            mainApp.showErrorAlert("Error", "Failed to filter books: " + e.getMessage());
        }
    }

    /**
     * Update cover preview for selected book.
     */
    private void updateCoverPreview(Book book) {
        if (book == null || book.getCoverImagePath() == null) {
            coverImageView.setImage(null);
            return;
        }

        try {
            File imageFile = new File(book.getCoverImagePath());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                coverImageView.setImage(image);
            } else {
                coverImageView.setImage(null);
            }
        } catch (Exception e) {
            logger.error("Failed to load cover image", e);
            coverImageView.setImage(null);
        }
    }

    /**
     * Update statistics label.
     */
    private void updateStats() {
        try {
            BookService.LibraryStats stats = bookService.getLibraryStats();
            statsLabel.setText(stats.toString());
        } catch (SQLException e) {
            logger.error("Failed to get statistics", e);
        }
    }

    /**
     * Handle add book button.
     */
    public void handleAddBook() {
        try {
            BookFormView dialog = new BookFormView(mainApp, null);
            Optional<Book> result = dialog.showAndWait();
            if (result.isPresent()) {
                refreshBooks();
                logger.info("Book added successfully: {}", result.get().getTitle());
            }
        } catch (Exception e) {
            logger.error("Failed to add book", e);
            mainApp.showErrorAlert("Error", "Failed to add book: " + e.getMessage());
        }
    }

    /**
     * Handle edit book button.
     */
    public void handleEditBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            mainApp.showInfoAlert("No Selection", "Please select a book to edit.");
            return;
        }

        try {
            BookFormView dialog = new BookFormView(mainApp, selectedBook);
            Optional<Book> result = dialog.showAndWait();
            if (result.isPresent()) {
                refreshBooks();
                logger.info("Book updated successfully: {}", result.get().getTitle());
            }
        } catch (Exception e) {
            logger.error("Failed to edit book", e);
            mainApp.showErrorAlert("Error", "Failed to edit book: " + e.getMessage());
        }
    }

    /**
     * Handle delete book button.
     */
    public void handleDeleteBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            mainApp.showInfoAlert("No Selection", "Please select a book to delete.");
            return;
        }

        boolean confirmed = mainApp.showConfirmDialog(
            "Confirm Delete",
            "Are you sure you want to delete \"" + selectedBook.getTitle() + "\"?"
        );

        if (confirmed) {
            try {
                bookService.deleteBook(selectedBook.getId());
                refreshBooks();
                mainApp.showInfoAlert("Success", "Book deleted successfully!");
            } catch (SQLException e) {
                logger.error("Failed to delete book", e);
                mainApp.showErrorAlert("Error", "Failed to delete book: " + e.getMessage());
            }
        }
    }

    /**
     * Refresh book list.
     */
    public void refreshBooks() {
        loadBooks();
        updateStats();
        loadCategoryFilter(categoryFilter);
    }

    /**
     * Show column customization dialog.
     */
    private void showColumnCustomization() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Customize Columns");
        dialog.setHeaderText("Select which columns to display");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Create checkboxes for each column
        CheckBox idCheck = new CheckBox("ID");
        idCheck.setSelected(bookTable.getColumns().contains(idCol));
        idCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(idCol, selected));

        CheckBox titleCheck = new CheckBox("Title");
        titleCheck.setSelected(bookTable.getColumns().contains(titleCol));
        titleCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(titleCol, selected));

        CheckBox authorsCheck = new CheckBox("Authors");
        authorsCheck.setSelected(bookTable.getColumns().contains(authorsCol));
        authorsCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(authorsCol, selected));

        CheckBox isbnCheck = new CheckBox("ISBN");
        isbnCheck.setSelected(bookTable.getColumns().contains(isbnCol));
        isbnCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(isbnCol, selected));

        CheckBox publisherCheck = new CheckBox("Publisher");
        publisherCheck.setSelected(bookTable.getColumns().contains(publisherCol));
        publisherCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(publisherCol, selected));

        CheckBox yearCheck = new CheckBox("Year");
        yearCheck.setSelected(bookTable.getColumns().contains(yearCol));
        yearCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(yearCol, selected));

        CheckBox categoryCheck = new CheckBox("Category");
        categoryCheck.setSelected(bookTable.getColumns().contains(categoryCol));
        categoryCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(categoryCol, selected));

        CheckBox shelfCheck = new CheckBox("Shelf Location");
        shelfCheck.setSelected(bookTable.getColumns().contains(shelfCol));
        shelfCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(shelfCol, selected));

        CheckBox tagsCheck = new CheckBox("Tags");
        tagsCheck.setSelected(bookTable.getColumns().contains(tagsCol));
        tagsCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(tagsCol, selected));

        CheckBox formatCheck = new CheckBox("Format");
        formatCheck.setSelected(bookTable.getColumns().contains(formatCol));
        formatCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(formatCol, selected));

        CheckBox readCheck = new CheckBox("Read Status");
        readCheck.setSelected(bookTable.getColumns().contains(readCol));
        readCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(readCol, selected));

        CheckBox ratingCheck = new CheckBox("Rating");
        ratingCheck.setSelected(bookTable.getColumns().contains(ratingCol));
        ratingCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(ratingCol, selected));

        CheckBox physicalLocationCheck = new CheckBox("Physical Location");
        physicalLocationCheck.setSelected(bookTable.getColumns().contains(physicalLocationCol));
        physicalLocationCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(physicalLocationCol, selected));

        CheckBox borrowedStatusCheck = new CheckBox("Borrowed Status");
        borrowedStatusCheck.setSelected(bookTable.getColumns().contains(borrowedStatusCol));
        borrowedStatusCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(borrowedStatusCol, selected));

        CheckBox borrowedToCheck = new CheckBox("Borrowed To");
        borrowedToCheck.setSelected(bookTable.getColumns().contains(borrowedToCol));
        borrowedToCheck.selectedProperty().addListener((obs, old, selected) -> toggleColumn(borrowedToCol, selected));

        content.getChildren().addAll(
            idCheck, titleCheck, authorsCheck, isbnCheck, publisherCheck,
            yearCheck, categoryCheck, shelfCheck, physicalLocationCheck, tagsCheck, formatCheck,
            readCheck, ratingCheck, borrowedStatusCheck, borrowedToCheck
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Toggle column visibility.
     */
    private <T> void toggleColumn(TableColumn<Book, T> column, boolean show) {
        if (show && !bookTable.getColumns().contains(column)) {
            // Add column in the correct position based on the defined order
            List<TableColumn<Book, ?>> allColumns = List.of(
                idCol, titleCol, authorsCol, isbnCol, publisherCol,
                yearCol, categoryCol, shelfCol, physicalLocationCol, borrowedStatusCol, borrowedToCol,
                tagsCol, formatCol, readCol, ratingCol
            );

            int targetIndex = 0;
            for (int i = 0; i < allColumns.size(); i++) {
                if (allColumns.get(i) == column) {
                    // Count how many columns before this one are currently visible
                    for (int j = 0; j < i; j++) {
                        if (bookTable.getColumns().contains(allColumns.get(j))) {
                            targetIndex++;
                        }
                    }
                    break;
                }
            }

            bookTable.getColumns().add(targetIndex, column);
        } else if (!show && bookTable.getColumns().contains(column)) {
            bookTable.getColumns().remove(column);
        }
    }

    /**
     * Get the view.
     */
    public BorderPane getView() {
        return view;
    }
}
