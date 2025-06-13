package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.exception.ValidationException;
import com.tanmaysharma.sqliteclone.model.Statement;

/**
 * Interface for validating SQL statements.
 * Ensures statements are well-formed and satisfy constraints.
 */
public interface QueryValidator {

    /**
     * Validates a statement.
     *
     * @param statement The statement to validate
     * @throws ValidationException if validation fails
     */
    void validate(Statement statement) throws ValidationException;

    /**
     * Validates an INSERT statement.
     *
     * @param statement The INSERT statement
     * @throws ValidationException if validation fails
     */
    void validateInsert(Statement statement) throws ValidationException;

    /**
     * Validates a SELECT statement.
     *
     * @param statement The SELECT statement
     * @throws ValidationException if validation fails
     */
    void validateSelect(Statement statement) throws ValidationException;

    /**
     * Checks if validation is enabled.
     *
     * @return True if validation is enabled
     */
    boolean isValidationEnabled();

    /**
     * Sets whether validation is enabled.
     *
     * @param enabled True to enable validation
     */
    void setValidationEnabled(boolean enabled);
}