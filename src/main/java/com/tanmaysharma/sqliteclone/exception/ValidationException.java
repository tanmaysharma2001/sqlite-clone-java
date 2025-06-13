package com.tanmaysharma.sqliteclone.exception;

/**
 * Exception thrown for validation errors.
 * Includes data validation and constraint violations.
 */
public class ValidationException extends DatabaseException {

    private final String fieldName;
    private final Object invalidValue;

    /**
     * Creates a new validation exception with the specified message.
     *
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Creates a new validation exception with field details.
     *
     * @param message The error message
     * @param fieldName The name of the invalid field
     * @param invalidValue The invalid value
     */
    public ValidationException(String message, String fieldName, Object invalidValue) {
        super(message, "VALIDATION_ERROR");
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    /**
     * Gets the name of the field that failed validation.
     *
     * @return The field name, or null if not specified
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the invalid value that caused the validation error.
     *
     * @return The invalid value, or null if not specified
     */
    public Object getInvalidValue() {
        return invalidValue;
    }
}