package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.model.Statement;

/**
 * Interface for executing SQL statements.
 * Handles the actual execution of parsed statements.
 */
public interface StatementExecutor {

    /**
     * Executes a statement.
     *
     * @param statement The statement to execute
     * @return The execution result
     * @throws QueryException if execution fails
     */
    ExecuteResult execute(Statement statement) throws QueryException;

    /**
     * Executes an INSERT statement.
     *
     * @param statement The INSERT statement
     * @return The execution result
     * @throws QueryException if execution fails
     */
    ExecuteResult executeInsert(Statement statement) throws QueryException;

    /**
     * Executes a SELECT statement.
     *
     * @param statement The SELECT statement
     * @return The execution result
     * @throws QueryException if execution fails
     */
    ExecuteResult executeSelect(Statement statement) throws QueryException;

    /**
     * Gets execution statistics.
     *
     * @return Statistics string
     */
    String getExecutionStatistics();
}