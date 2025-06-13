package com.tanmaysharma.sqliteclone.exception;

/**
 * Exception thrown for storage-related errors.
 * Includes file I/O, page management, and disk operations.
 */
public class StorageException extends DatabaseException {

    /**
     * Creates a new storage exception with the specified message.
     *
     * @param message The error message
     */
    public StorageException(String message) {
        super(message, "STORAGE_ERROR");
    }

    /**
     * Creates a new storage exception with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause, "STORAGE_ERROR");
    }

    /**
     * Creates a new storage exception with the specified message and error code.
     *
     * @param message The error message
     * @param errorCode The specific storage error code
     */
    public StorageException(String message, String errorCode) {
        super(message, errorCode);
    }
}