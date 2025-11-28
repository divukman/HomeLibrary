package com.homelibrary.ui;

import com.homelibrary.dao.Database;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application class for Home Library.
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private Stage primaryStage;
    private BorderPane rootLayout;
    private BookListView bookListView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Home Library Manager");

        // Initialize database
        try {
            Database.getInstance();
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            showErrorAlert("Database Error", "Failed to initialize database: " + e.getMessage());
            return;
        }

        initRootLayout();
        showBookListView();
    }

    /**
     * Initialize the root layout with menu bar.
     */
    private void initRootLayout() {
        rootLayout = new BorderPane();

        // Create menu bar
        MenuBar menuBar = createMenuBar();
        rootLayout.setTop(menuBar);

        Scene scene = new Scene(rootLayout, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Create application menu bar.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> {
            if (bookListView != null) {
                bookListView.refreshBooks();
            }
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            cleanup();
            primaryStage.close();
        });

        fileMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), exitItem);

        // Book menu
        Menu bookMenu = new Menu("Book");

        MenuItem addBookItem = new MenuItem("Add Book");
        addBookItem.setOnAction(e -> {
            if (bookListView != null) {
                bookListView.handleAddBook();
            }
        });

        MenuItem editBookItem = new MenuItem("Edit Book");
        editBookItem.setOnAction(e -> {
            if (bookListView != null) {
                bookListView.handleEditBook();
            }
        });

        MenuItem deleteBookItem = new MenuItem("Delete Book");
        deleteBookItem.setOnAction(e -> {
            if (bookListView != null) {
                bookListView.handleDeleteBook();
            }
        });

        bookMenu.getItems().addAll(addBookItem, editBookItem, deleteBookItem);

        // Help menu
        Menu helpMenu = new Menu("Help");

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, bookMenu, helpMenu);
        return menuBar;
    }

    /**
     * Show the book list view.
     */
    private void showBookListView() {
        try {
            bookListView = new BookListView(this);
            rootLayout.setCenter(bookListView.getView());
        } catch (Exception e) {
            logger.error("Failed to load book list view", e);
            showErrorAlert("Error", "Failed to load book list: " + e.getMessage());
        }
    }

    /**
     * Show about dialog.
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Home Library Manager");
        alert.setHeaderText("Home Library Manager v1.0");
        alert.setContentText(
            "A JavaFX application for managing your personal book collection.\n\n" +
            "Features:\n" +
            "- Book management with authors and categories\n" +
            "- Cover image support\n" +
            "- Amazon API integration (when configured)\n" +
            "- SQLite database storage"
        );
        alert.showAndWait();
    }

    /**
     * Show error alert dialog.
     */
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show information alert dialog.
     */
    public void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show confirmation dialog.
     */
    public boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    /**
     * Get primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        try {
            Database.getInstance().close();
            logger.info("Database connection closed");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    @Override
    public void stop() {
        cleanup();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
