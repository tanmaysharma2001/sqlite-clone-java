package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.model.Statement;

/**
 * Interface for the query engine.
 * Handles SQL parsing, validation, and execution.
 */
public interface QueryEngine {

    /**
     * Parses and executes a SQL statement.
     *
     * @param query The SQL query string
     * @return The execution result
     * @throws QueryException if parsing or execution fails
     */
    ExecuteResult executeQuery(String query) throws QueryException;

    /**
     * Parses a SQL statement without executing it.
     *
     * @param query The SQL query string
     * @return The parsed statement
     * @throws QueryException if parsing fails
     */
    Statement parseStatement(String query) throws QueryException;

    /**
     * Executes a pre-parsed statement.
     *
     * @param statement The statement to execute
     * @return The execution result
     * @throws QueryException if execution fails
     */
    ExecuteResult executeStatement(Statement statement) throws QueryException;

    /**
     * Validates a SQL statement.
     *
     * @param statement The statement to validate
     * @throws QueryException if validation fails
     */
    void validateStatement(Statement statement) throws QueryException;

    /**
     * Gets query execution statistics.
     *
     * @return Statistics string
     */
    String getQueryStatistics();
}