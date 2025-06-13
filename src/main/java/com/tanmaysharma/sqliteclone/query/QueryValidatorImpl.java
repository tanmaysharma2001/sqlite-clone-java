package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.enums.StatementType;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import com.tanmaysharma.sqliteclone.model.Row;
import com.tanmaysharma.sqliteclone.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the query validator.
 * Validates SQL statements for correctness and constraint compliance.
 */
public class QueryValidatorImpl implements QueryValidator {

    private static final Logger logger = LoggerFactory.getLogger(QueryValidatorImpl.class);

    private boolean validationEnabled = DatabaseConfig.VALIDATION_ENABLED;

    @Override
    public void validate(Statement statement) throws ValidationException {
        if (!validationEnabled) {
            return;
        }

        if (statement == null) {
            throw new ValidationException("Statement cannot be null");
        }

        switch (statement.getType()) {
            case STATEMENT_INSERT:
                validateInsert(statement);
                break;
            case STATEMENT_SELECT:
                validateSelect(statement);
                break;
            default:
                throw new ValidationException("Unsupported statement type: " + statement.getType());
        }

        logger.debug("Validated {} statement", statement.getType());
    }

    @Override
    public void validateInsert(Statement statement) throws ValidationException {
        if (statement.getType() != StatementType.STATEMENT_INSERT) {
            throw new ValidationException("Expected INSERT statement");
        }

        Row row = statement.getRowToInsert();
        if (row == null) {
            throw new ValidationException("INSERT statement missing row data");
        }

        // Row validation is handled in the Row constructor
        // Additional business logic validation can be added here

        logger.debug("Validated INSERT statement for ID: {}", row.getId());
    }

    @Override
    public void validateSelect(Statement statement) throws ValidationException {
        if (statement.getType() != StatementType.STATEMENT_SELECT) {
            throw new ValidationException("Expected SELECT statement");
        }

        // Validate WHERE clause if present
        if (statement.hasWhereClause()) {
            validateWhereClause(statement.getWhereClause());
        }

        // Validate column selection if present
        if (statement.hasSpecificColumns()) {
            validateSelectColumns(statement.getSelectColumns());
        }

        logger.debug("Validated SELECT statement with WHERE: {}, Columns: {}",
                statement.hasWhereClause(), statement.hasSpecificColumns());
    }

    @Override
    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    @Override
    public void setValidationEnabled(boolean enabled) {
        this.validationEnabled = enabled;
        logger.info("Validation {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Validates a WHERE clause.
     *
     * @param whereClause The WHERE clause to validate
     * @throws ValidationException if invalid
     */
    private void validateWhereClause(String whereClause) throws ValidationException {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            throw new ValidationException("WHERE clause cannot be empty");
        }

        // Simple validation for supported WHERE clauses
        String normalizedClause = whereClause.toLowerCase().trim();

        // Check for basic "id = number" pattern
        if (!normalizedClause.matches("\\s*id\\s*=\\s*\\d+\\s*")) {
            throw new ValidationException("Unsupported WHERE clause format: " + whereClause +
                    ". Only 'id = number' format is supported.");
        }

        logger.debug("Validated WHERE clause: {}", whereClause);
    }

    /**
     * Validates column selection.
     *
     * @param columns The columns to validate
     * @throws ValidationException if invalid
     */
    private void validateSelectColumns(String[] columns) throws ValidationException {
        if (columns == null || columns.length == 0) {
            return; // SELECT * is valid
        }

        // Define supported column names
        String[] supportedColumns = {"id", "username", "email", "*"};

        for (String column : columns) {
            if (column == null || column.trim().isEmpty()) {
                throw new ValidationException("Column name cannot be empty");
            }

            String cleanColumn = column.trim().toLowerCase();
            boolean isSupported = false;

            for (String supported : supportedColumns) {
                if (supported.equals(cleanColumn)) {
                    isSupported = true;
                    break;
                }
            }

            if (!isSupported) {
                throw new ValidationException("Unsupported column: " + column +
                        ". Supported columns are: id, username, email, *");
            }
        }

        logger.debug("Validated {} columns", columns.length);
    }
}