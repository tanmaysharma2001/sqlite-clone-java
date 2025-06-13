package com.tanmaysharma.sqliteclone.exception;

/**
 * Base exception class for all database-related operations.
 * Provides a common hierarchy for database errors.
 */
public class DatabaseException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a new database exception with the specified message.
     *
     * @param message The error message
     */
    public DatabaseException(String message) {
        super(message);
        this.errorCode = "DB_ERROR";
    }

    /**
     * Creates a new database exception with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DB_ERROR";
    }

    /**
     * Creates a new database exception with the specified message and error code.
     *
     * @param message The error message
     * @param errorCode The error code
     */
    public DatabaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new database exception with the specified message, cause, and error code.
     *
     * @param message The error message
     * @param cause The cause of the exception
     * @param errorCode The error code
     */
    public DatabaseException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}