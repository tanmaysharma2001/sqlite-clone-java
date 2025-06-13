package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.model.Statement;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of the query engine.
 * Coordinates parsing, validation, and execution of SQL statements.
 */
public class QueryEngineImpl implements QueryEngine {

    private static final Logger logger = LoggerFactory.getLogger(QueryEngineImpl.class);

    private final StorageEngine storageEngine;
    private final StatementParser parser;
    private final StatementExecutor executor;
    private final QueryValidator validator;
    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    /**
     * Creates a new query engine with the specified storage engine.
     *
     * @param storageEngine The storage engine to use
     */
    public QueryEngineImpl(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
        this.parser = new StatementParserImpl();
        this.executor = new StatementExecutorImpl(storageEngine);
        this.validator = new QueryValidatorImpl();

        logger.info("Query engine initialized");
    }

    @Override
    public ExecuteResult executeQuery(String query) throws QueryException {
        if (query == null || query.trim().isEmpty()) {
            throw new QueryException("Query cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();

        try {
            queryCount.incrementAndGet();

            // Parse the statement
            Statement statement = parseStatement(query);

            // Validate the statement
            if (DatabaseConfig.VALIDATION_ENABLED) {
                validateStatement(statement);
            }

            // Execute the statement
            ExecuteResult result = executeStatement(statement);

            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTime.addAndGet(executionTime);

            logger.debug("Executed query in {}ms: {}", executionTime, query);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTime.addAndGet(executionTime);

            logger.error("Query execution failed after {}ms: {}", executionTime, query, e);

            if (e instanceof QueryException) {
                throw e;
            } else {
                throw new QueryException("Query execution failed: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Statement parseStatement(String query) throws QueryException {
        try {
            return parser.parse(query);
        } catch (Exception e) {
            throw new QueryException("Failed to parse query: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecuteResult executeStatement(Statement statement) throws QueryException {
        try {
            return executor.execute(statement);
        } catch (Exception e) {
            throw new QueryException("Failed to execute statement: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateStatement(Statement statement) throws QueryException {
        try {
            validator.validate(statement);
        } catch (Exception e) {
            throw new QueryException("Statement validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getQueryStatistics() {
        long queries = queryCount.get();
        long totalTime = totalExecutionTime.get();

        if (queries == 0) {
            return "No queries executed";
        }

        double avgTime = (double) totalTime / queries;

        return String.format(
                "Query Statistics:\n" +
                        "  Total queries: %d\n" +
                        "  Total execution time: %d ms\n" +
                        "  Average execution time: %.2f ms\n" +
                        "  %s\n" +
                        "  %s",
                queries, totalTime, avgTime,
                executor.getExecutionStatistics(),
                storageEngine.getStatistics()
        );
    }
}