package com.tanmaysharma.sqliteclone;

/**
 * Base exception class for database operations.
 */
public class DatabaseException extends RuntimeException {
    /**
     * Creates a new database exception with the specified message.
     *
     * @param message The error message
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Creates a new database exception with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
