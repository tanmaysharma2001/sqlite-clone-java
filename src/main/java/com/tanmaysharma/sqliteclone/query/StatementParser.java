package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.PrepareResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.model.Statement;

/**
 * Interface for parsing SQL statements.
 * Converts SQL text into Statement objects.
 */
public interface StatementParser {

    /**
     * Parses a SQL statement from text.
     *
     * @param query The SQL query string
     * @return The parsed statement
     * @throws QueryException if parsing fails
     */
    Statement parse(String query) throws QueryException;

    /**
     * Gets the result of the last parse operation.
     *
     * @return The prepare result
     */
    PrepareResult getLastResult();

    /**
     * Checks if a query string represents a valid SQL statement.
     *
     * @param query The query string
     * @return True if valid
     */
    boolean isValidQuery(String query);
}
