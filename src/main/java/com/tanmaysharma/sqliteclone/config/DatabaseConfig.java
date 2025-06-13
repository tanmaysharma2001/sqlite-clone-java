package com.tanmaysharma.sqliteclone.config;

/**
 * Database configuration constants and settings.
 * Centralized configuration for all database-related parameters.
 */
public final class DatabaseConfig {

    // Database Structure
    public static final int PAGE_SIZE = getIntProperty("database.page.size", 4096);
    public static final int MAX_PAGES = getIntProperty("database.max.pages", 100);
    public static final int ROW_SIZE = getIntProperty("database.row.size", 292);
    public static final int TABLE_MAX_ROWS = getIntProperty("database.table.max.rows", 1000);
    public static final int ROWS_PER_PAGE = PAGE_SIZE / ROW_SIZE;

    // Connection Management
    public static final int CONNECTION_TIMEOUT_MS = getIntProperty("database.connection.timeout.ms", 5000);
    public static final int MAX_CONNECTIONS = getIntProperty("database.max.connections", 10);

    // Row Structure
    public static final int ID_SIZE = 4;
    public static final int MAX_USERNAME_LENGTH = 32;
    public static final int MAX_EMAIL_LENGTH = 256;
    public static final int ID_OFFSET = 0;
    public static final int USERNAME_OFFSET = ID_OFFSET + ID_SIZE;
    public static final int EMAIL_OFFSET = USERNAME_OFFSET + MAX_USERNAME_LENGTH;

    // Storage Configuration
    public static final boolean CACHE_ENABLED = getBooleanProperty("storage.cache.enabled", true);
    public static final int CACHE_SIZE_MB = getIntProperty("storage.cache.size.mb", 64);
    public static final boolean SYNC_ON_WRITE = getBooleanProperty("storage.sync.on.write", true);
    public static final boolean BACKUP_ENABLED = getBooleanProperty("storage.backup.enabled", false);
    public static final int BACKUP_INTERVAL_MINUTES = getIntProperty("storage.backup.interval.minutes", 30);

    // Query Configuration
    public static final int QUERY_TIMEOUT_MS = getIntProperty("query.timeout.ms", 30000);
    public static final int MAX_RESULT_SIZE = getIntProperty("query.max.result.size", 1000);
    public static final boolean VALIDATION_ENABLED = getBooleanProperty("query.validation.enabled", true);

    // Performance Configuration
    public static final boolean METRICS_ENABLED = getBooleanProperty("performance.metrics.enabled", true);
    public static final boolean CACHE_STATISTICS = getBooleanProperty("performance.cache.statistics", true);
    public static final boolean QUERY_STATISTICS = getBooleanProperty("performance.query.statistics", true);

    /**
     * Private constructor to prevent instantiation.
     */
    private DatabaseConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets an integer property with a default value.
     *
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value or default
     */
    private static int getIntProperty(String key, int defaultValue) {
        try {
            String value = System.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a boolean property with a default value.
     *
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value or default
     */
    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Validates the current configuration.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public static void validate() {
        if (PAGE_SIZE <= 0) {
            throw new IllegalStateException("Page size must be positive");
        }

        if (MAX_PAGES <= 0) {
            throw new IllegalStateException("Max pages must be positive");
        }

        if (ROW_SIZE <= 0) {
            throw new IllegalStateException("Row size must be positive");
        }

        if (ROWS_PER_PAGE <= 0) {
            throw new IllegalStateException("Invalid row size - no rows fit in page");
        }

        if (TABLE_MAX_ROWS <= 0) {
            throw new IllegalStateException("Table max rows must be positive");
        }

        if (MAX_CONNECTIONS <= 0) {
            throw new IllegalStateException("Max connections must be positive");
        }

        if (CONNECTION_TIMEOUT_MS <= 0) {
            throw new IllegalStateException("Connection timeout must be positive");
        }
    }

    /**
     * Returns a string representation of the current configuration.
     *
     * @return Configuration summary
     */
    public static String getConfigurationSummary() {
        return String.format(
                "DatabaseConfig{pageSize=%d, maxPages=%d, rowSize=%d, rowsPerPage=%d, " +
                        "tableMaxRows=%d, maxConnections=%d, connectionTimeoutMs=%d}",
                PAGE_SIZE, MAX_PAGES, ROW_SIZE, ROWS_PER_PAGE,
                TABLE_MAX_ROWS, MAX_CONNECTIONS, CONNECTION_TIMEOUT_MS
        );
    }
}