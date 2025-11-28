package com.homelibrary.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for interacting with Amazon Product Advertising API.
 *
 * This implementation provides a foundation for Amazon PA-API 5.0 integration.
 * Note: Full implementation requires proper AWS signature version 4 authentication.
 */
public class AmazonApiService {
    private static final Logger logger = LoggerFactory.getLogger(AmazonApiService.class);
    private static final String SERVICE_NAME = "ProductAdvertisingAPI";
    private static final String AWS_ALGORITHM = "AWS4-HMAC-SHA256";

    private final ConfigService configService;
    private final OkHttpClient httpClient;
    private String region;
    private String accessKey;
    private String secretKey;
    private String associateTag;

    public AmazonApiService() {
        this.configService = ConfigService.getInstance();
        this.httpClient = new OkHttpClient();
        loadConfiguration();
    }

    /**
     * Load Amazon API configuration.
     */
    private void loadConfiguration() {
        this.accessKey = configService.getAmazonAccessKey();
        this.secretKey = configService.getAmazonSecretKey();
        this.associateTag = configService.getAmazonAssociateTag();
        this.region = configService.getAmazonRegion();
    }

    /**
     * Check if API is properly configured.
     */
    public boolean isConfigured() {
        return configService.isAmazonApiConfigured();
    }

    /**
     * Search for books by title and author.
     *
     * @param title Book title
     * @param author Author name (optional)
     * @return List of search results
     */
    public List<BookSearchResult> searchBooks(String title, String author) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Amazon API is not configured");
        }

        logger.info("Searching Amazon for: title='{}', author='{}'", title, author);

        // Build search keywords
        String keywords = title;
        if (author != null && !author.isEmpty()) {
            keywords += " " + author;
        }

        // For demonstration: This is a simplified placeholder
        // Full implementation requires AWS Signature Version 4 authentication
        List<BookSearchResult> results = new ArrayList<>();

        logger.warn("Amazon API search is not fully implemented - requires PA-API 5.0 credentials and signing");

        return results;
    }

    /**
     * Get book details by ISBN.
     *
     * @param isbn ISBN-10 or ISBN-13
     * @return Book details
     */
    public Optional<BookSearchResult> getBookByIsbn(String isbn) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Amazon API is not configured");
        }

        logger.info("Looking up ISBN: {}", isbn);

        // Placeholder for PA-API 5.0 GetItems operation
        logger.warn("Amazon API ISBN lookup is not fully implemented");

        return Optional.empty();
    }

    /**
     * Download cover image from URL.
     *
     * @param imageUrl URL of the image
     * @param bookId Book ID for filename
     * @return Path to downloaded image
     */
    public String downloadCoverImage(String imageUrl, Integer bookId) throws IOException {
        logger.info("Downloading cover image from: {}", imageUrl);

        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download image: " + response.code());
            }

            String coversDir = configService.getCoversDirectory();
            File dir = new File(coversDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = bookId + ".jpg";
            Path targetPath = Paths.get(coversDir, fileName);

            if (response.body() != null) {
                Files.copy(response.body().byteStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Downloaded cover image to: {}", targetPath);
                return targetPath.toString();
            } else {
                throw new IOException("Empty response body");
            }
        }
    }

    /**
     * Result object for book search.
     */
    public static class BookSearchResult {
        private String title;
        private String subtitle;
        private List<String> authors;
        private String isbn10;
        private String isbn13;
        private String publisher;
        private Integer yearPublished;
        private String coverImageUrl;
        private String asin;

        public BookSearchResult() {
            this.authors = new ArrayList<>();
        }

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public List<String> getAuthors() {
            return authors;
        }

        public void setAuthors(List<String> authors) {
            this.authors = authors;
        }

        public String getIsbn10() {
            return isbn10;
        }

        public void setIsbn10(String isbn10) {
            this.isbn10 = isbn10;
        }

        public String getIsbn13() {
            return isbn13;
        }

        public void setIsbn13(String isbn13) {
            this.isbn13 = isbn13;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public Integer getYearPublished() {
            return yearPublished;
        }

        public void setYearPublished(Integer yearPublished) {
            this.yearPublished = yearPublished;
        }

        public String getCoverImageUrl() {
            return coverImageUrl;
        }

        public void setCoverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
        }

        public String getAsin() {
            return asin;
        }

        public void setAsin(String asin) {
            this.asin = asin;
        }

        @Override
        public String toString() {
            return String.format("%s by %s (ISBN: %s)", title, String.join(", ", authors),
                    isbn13 != null ? isbn13 : isbn10);
        }
    }

    /**
     * Helper method to create AWS Signature Version 4.
     * This is a simplified version - full implementation needed for production.
     */
    private String createSignature(String stringToSign, String secretKey, String dateStamp, String region) {
        try {
            byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
            byte[] kDate = hmacSHA256(dateStamp, kSecret);
            byte[] kRegion = hmacSHA256(region, kDate);
            byte[] kService = hmacSHA256(SERVICE_NAME, kRegion);
            byte[] kSigning = hmacSHA256("aws4_request", kService);
            byte[] signature = hmacSHA256(stringToSign, kSigning);

            return bytesToHex(signature);
        } catch (Exception e) {
            logger.error("Failed to create signature", e);
            return "";
        }
    }

    /**
     * HMAC-SHA256 helper.
     */
    private byte[] hmacSHA256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
