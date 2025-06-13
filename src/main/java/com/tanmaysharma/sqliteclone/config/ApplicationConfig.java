package com.tanmaysharma.sqliteclone.config;

/**
 * Application-wide configuration settings.
 * Contains CLI and general application configurations.
 */
public final class ApplicationConfig {

    // CLI Configuration
    public static final String CLI_PROMPT = getStringProperty("cli.prompt", "db > ");
    public static final int MAX_INPUT_LENGTH = getIntProperty("cli.max.input.length", 1024);
    public static final boolean HISTORY_ENABLED = getBooleanProperty("cli.history.enabled", true);
    public static final int HISTORY_SIZE = getIntProperty("cli.history.size", 100);

    // Application Information
    public static final String APPLICATION_NAME = "SQLite Clone";
    public static final String APPLICATION_VERSION = "2.0.0";
    public static final String WELCOME_MESSAGE = String.format(
            "%s v%s%nEnter \".help\" for usage hints.",
            APPLICATION_NAME, APPLICATION_VERSION
    );

    // Command Patterns - Fixed regex patterns
    // Allow negative numbers in INSERT pattern for proper validation
    public static final String INSERT_PATTERN = "insert\\s+(-?\\d+)\\s+([^\\s]+)\\s+(.+)";
    // Fixed SELECT pattern to properly handle WHERE clause with negative lookahead
    public static final String SELECT_PATTERN = "select(?:\\s+(?!where\\s)([^\\s].*?))?(?:\\s+where\\s+(.+))?\\s*$";

    // Help Messages
    public static final String HELP_MESSAGE = """
        Special commands:
          .help                - Display this help message
          .exit                - Exit the program
          .stats               - Display database statistics
          .info                - Display database information
          .clear               - Clear the screen
        
        SQL commands:
          insert <id> <username> <email>
            - Insert a new row with the specified values
          select [* | columns] [where condition]
            - Display rows in the table
            - Example: select where id = 1
        """;

    /**
     * Private constructor to prevent instantiation.
     */
    private ApplicationConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets a string property with a default value.
     *
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value or default
     */
    private static String getStringProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
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
     * Returns the welcome message with database filename.
     *
     * @param filename The database filename
     * @return Formatted welcome message
     */
    public static String getWelcomeMessage(String filename) {
        return String.format("%s%nConnected to %s", WELCOME_MESSAGE, filename);
    }
}