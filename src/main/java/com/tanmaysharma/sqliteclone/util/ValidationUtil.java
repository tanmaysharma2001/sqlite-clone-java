package com.tanmaysharma.sqliteclone.util;

import com.tanmaysharma.sqliteclone.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Utility class for common validation operations.
 */
public final class ValidationUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates that a string is not null or empty.
     *
     * @param value The string to validate
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new ValidationException(
                    String.format("%s cannot be null or empty", fieldName),
                    fieldName, value
            );
        }
    }

    /**
     * Validates that a number is positive.
     *
     * @param value The number to validate
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(
                    String.format("%s must be positive", fieldName),
                    fieldName, value
            );
        }
    }

    /**
     * Validates that a number is non-negative.
     *
     * @param value The number to validate
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(
                    String.format("%s cannot be negative", fieldName),
                    fieldName, value
            );
        }
    }

    /**
     * Validates that a string doesn't exceed maximum length.
     *
     * @param value The string to validate
     * @param maxLength The maximum allowed length
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void requireMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(
                    String.format("%s exceeds maximum length of %d", fieldName, maxLength),
                    fieldName, value
            );
        }
    }

    /**
     * Validates that a file path is valid and writable.
     *
     * @param filename The filename to validate
     * @throws ValidationException if validation fails
     */
    public static void validateDatabaseFile(String filename) {
        requireNonEmpty(filename, "filename");

        File file = new File(filename);
        File parentDir = file.getParentFile();

        // Ensure parent directory exists or can be created
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new ValidationException(
                        "Cannot create directory: " + parentDir,
                        "filename", filename
                );
            }
        }

        // Check if file can be created or is writable
        if (file.exists() && !file.canWrite()) {
            throw new ValidationException(
                    "Cannot write to file: " + filename,
                    "filename", filename
            );
        }
    }

    /**
     * Validates that an object is not null.
     *
     * @param value The object to validate
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(
                    String.format("%s cannot be null", fieldName),
                    fieldName, value
            );
        }
    }

    /**
     * Validates a basic email format.
     *
     * @param email The email to validate
     * @param fieldName The field name for error messages
     * @throws ValidationException if validation fails
     */
    public static void validateEmail(String email, String fieldName) {
        if (email != null && !email.isEmpty() && !email.contains("@")) {
            throw new ValidationException(
                    String.format("%s must contain @ symbol", fieldName),
                    fieldName, email
            );
        }
    }
}