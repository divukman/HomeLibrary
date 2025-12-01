package com.homelibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Service for managing application configuration.
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_FILE = "config.properties";

    private static ConfigService instance;
    private final Properties properties;

    private ConfigService() {
        this.properties = new Properties();
        loadConfiguration();
    }

    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Load configuration from file.
     */
    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
                logger.info("Configuration loaded from {}", CONFIG_FILE);
            } catch (IOException e) {
                logger.error("Failed to load configuration", e);
            }
        } else {
            logger.info("Configuration file not found, using defaults");
            createDefaultConfiguration();
        }
    }

    /**
     * Create default configuration file.
     */
    private void createDefaultConfiguration() {
        // Amazon API configuration
        properties.setProperty("amazon.api.access.key", "");
        properties.setProperty("amazon.api.secret.key", "");
        properties.setProperty("amazon.api.associate.tag", "");
        properties.setProperty("amazon.api.region", "us-east-1");

        // Application settings
        properties.setProperty("covers.directory", "covers");
        properties.setProperty("database.file", "homelibrary.db");

        saveConfiguration();
    }

    /**
     * Save configuration to file.
     */
    public void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Home Library Configuration");
            logger.info("Configuration saved to {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    /**
     * Get property value.
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get property value with default.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Set property value.
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Check if Amazon API is configured.
     */
    public boolean isAmazonApiConfigured() {
        String accessKey = getProperty("amazon.api.access.key");
        String secretKey = getProperty("amazon.api.secret.key");
        String associateTag = getProperty("amazon.api.associate.tag");

        return accessKey != null && !accessKey.isEmpty() &&
               secretKey != null && !secretKey.isEmpty() &&
               associateTag != null && !associateTag.isEmpty();
    }

    /**
     * Get covers directory path.
     */
    public String getCoversDirectory() {
        return getProperty("covers.directory", "covers");
    }

    /**
     * Get Amazon API access key.
     */
    public String getAmazonAccessKey() {
        return getProperty("amazon.api.access.key");
    }

    /**
     * Get Amazon API secret key.
     */
    public String getAmazonSecretKey() {
        return getProperty("amazon.api.secret.key");
    }

    /**
     * Get Amazon API associate tag.
     */
    public String getAmazonAssociateTag() {
        return getProperty("amazon.api.associate.tag");
    }

    /**
     * Get Amazon API region.
     */
    public String getAmazonRegion() {
        return getProperty("amazon.api.region", "us-east-1");
    }

    /**
     * Save column visibility state.
     */
    public void saveColumnVisibility(String columnName, boolean visible) {
        setProperty("column.visible." + columnName, String.valueOf(visible));
        saveConfiguration();
    }

    /**
     * Get column visibility state.
     */
    public boolean isColumnVisible(String columnName, boolean defaultValue) {
        String value = getProperty("column.visible." + columnName);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
