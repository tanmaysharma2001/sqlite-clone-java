package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.enums.StatementType;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.exception.StorageException;
import com.tanmaysharma.sqliteclone.model.Row;
import com.tanmaysharma.sqliteclone.model.Statement;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of the statement executor.
 * Executes parsed SQL statements using the storage engine.
 */
public class StatementExecutorImpl implements StatementExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StatementExecutorImpl.class);

    private final StorageEngine storageEngine;
    private final AtomicLong insertCount = new AtomicLong(0);
    private final AtomicLong selectCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * Creates a new statement executor.
     *
     * @param storageEngine The storage engine to use
     */
    public StatementExecutorImpl(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    @Override
    public ExecuteResult execute(Statement statement) throws QueryException {
        if (statement == null) {
            throw new QueryException("Statement cannot be null");
        }

        try {
            ExecuteResult result;

            switch (statement.getType()) {
                case STATEMENT_INSERT:
                    result = executeInsert(statement);
                    break;
                case STATEMENT_SELECT:
                    result = executeSelect(statement);
                    break;
                default:
                    throw new QueryException("Unsupported statement type: " + statement.getType());
            }

            if (result != ExecuteResult.EXECUTE_SUCCESS) {
                errorCount.incrementAndGet();
            }

            return result;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            throw new QueryException("Statement execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecuteResult executeInsert(Statement statement) throws QueryException {
        if (statement.getType() != StatementType.STATEMENT_INSERT) {
            throw new QueryException("Expected INSERT statement");
        }

        Row row = statement.getRowToInsert();
        if (row == null) {
            throw new QueryException("INSERT statement missing row data");
        }

        try {
            insertCount.incrementAndGet();

            if (storageEngine.isTableFull()) {
                return ExecuteResult.EXECUTE_TABLE_FULL;
            }

            if (storageEngine.rowExists(row.getId())) {
                return ExecuteResult.EXECUTE_DUPLICATE_KEY;
            }

            storageEngine.insertRow(row);

            logger.debug("Inserted row with ID: {}", row.getId());
            return ExecuteResult.EXECUTE_SUCCESS;

        } catch (StorageException e) {
            logger.error("Storage error during INSERT: {}", e.getMessage());
            return ExecuteResult.EXECUTE_FAIL;
        } catch (Exception e) {
            logger.error("Unexpected error during INSERT: {}", e.getMessage(), e);
            return ExecuteResult.EXECUTE_FAIL;
        }
    }

    @Override
    public ExecuteResult executeSelect(Statement statement) throws QueryException {
        if (statement.getType() != StatementType.STATEMENT_SELECT) {
            throw new QueryException("Expected SELECT statement");
        }

        try {
            selectCount.incrementAndGet();

            if (statement.hasWhereClause()) {
                return executeSelectWithWhere(statement);
            } else {
                return executeSelectAll(statement);
            }

        } catch (StorageException e) {
            logger.error("Storage error during SELECT: {}", e.getMessage());
            return ExecuteResult.EXECUTE_FAIL;
        } catch (Exception e) {
            logger.error("Unexpected error during SELECT: {}", e.getMessage(), e);
            return ExecuteResult.EXECUTE_FAIL;
        }
    }

    @Override
    public String getExecutionStatistics() {
        return String.format(
                "Execution Statistics: %d INSERTs, %d SELECTs, %d errors",
                insertCount.get(), selectCount.get(), errorCount.get()
        );
    }

    /**
     * Executes a SELECT statement with a WHERE clause.
     *
     * @param statement The SELECT statement
     * @return The execution result
     * @throws QueryException if execution fails
     */
    private ExecuteResult executeSelectWithWhere(Statement statement) throws QueryException {
        String whereClause = statement.getWhereClause();

        // Simple WHERE clause parsing for "id = value"
        if (whereClause.toLowerCase().matches("\\s*id\\s*=\\s*\\d+\\s*")) {
            try {
                int id = extractIdFromWhere(whereClause);
                Row row = storageEngine.getRowById(id);

                if (row != null) {
                    printRow(row);
                    logger.debug("Found row with ID: {}", id);
                } else {
                    logger.debug("No row found with ID: {}", id);
                }

                return ExecuteResult.EXECUTE_SUCCESS;

            } catch (Exception e) {
                logger.error("Error processing WHERE clause: {}", whereClause, e);
                return ExecuteResult.EXECUTE_FAIL;
            }
        } else {
            throw new QueryException("Unsupported WHERE clause: " + whereClause);
        }
    }

    /**
     * Executes a SELECT statement without WHERE clause (select all).
     *
     * @param statement The SELECT statement
     * @return The execution result
     * @throws QueryException if execution fails
     */
    private ExecuteResult executeSelectAll(Statement statement) throws QueryException {
        try {
            List<Row> rows = storageEngine.getAllRows();

            for (Row row : rows) {
                printRow(row);
            }

            logger.debug("Selected {} rows", rows.size());
            return ExecuteResult.EXECUTE_SUCCESS;

        } catch (StorageException e) {
            throw new QueryException("Failed to retrieve rows", e);
        }
    }

    /**
     * Extracts ID value from a simple WHERE clause.
     *
     * @param whereClause The WHERE clause
     * @return The extracted ID
     * @throws QueryException if parsing fails
     */
    private int extractIdFromWhere(String whereClause) throws QueryException {
        try {
            String[] parts = whereClause.split("=");
            if (parts.length != 2) {
                throw new QueryException("Invalid WHERE clause format");
            }

            return Integer.parseInt(parts[1].trim());

        } catch (NumberFormatException e) {
            throw new QueryException("Invalid ID in WHERE clause", e);
        }
    }

    /**
     * Prints a row to the console.
     *
     * @param row The row to print
     */
    private void printRow(Row row) {
        if (row != null) {
            System.out.println(row.toDisplayString());
        }
    }
}
