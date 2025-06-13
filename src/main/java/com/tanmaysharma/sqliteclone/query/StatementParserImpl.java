package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.config.ApplicationConfig;
import com.tanmaysharma.sqliteclone.enums.PrepareResult;
import com.tanmaysharma.sqliteclone.enums.StatementType;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.model.Row;
import com.tanmaysharma.sqliteclone.model.Statement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the statement parser.
 * Parses SQL text into Statement objects using regex patterns.
 */
public class StatementParserImpl implements StatementParser {

    private static final Logger logger = LoggerFactory.getLogger(StatementParserImpl.class);

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            ApplicationConfig.INSERT_PATTERN, Pattern.CASE_INSENSITIVE
    );

    // More specific SELECT pattern that handles WHERE clause properly
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "select(?:\\s+(?!where\\s)([^\\s].*?))?(?:\\s+where\\s+(.+))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private PrepareResult lastResult = PrepareResult.PREPARE_SUCCESS;

    @Override
    public Statement parse(String query) throws QueryException {
        if (StringUtils.isBlank(query)) {
            lastResult = PrepareResult.PREPARE_SYNTAX_ERROR;
            throw new QueryException("Query cannot be null or empty");
        }

        String trimmedQuery = query.trim();

        if (trimmedQuery.length() > ApplicationConfig.MAX_INPUT_LENGTH) {
            lastResult = PrepareResult.PREPARE_TOO_LONG;
            throw new QueryException("Query exceeds maximum length");
        }

        // Determine statement type
        String firstWord = trimmedQuery.split("\\s+")[0].toLowerCase();
        StatementType type = StatementType.fromKeyword(firstWord);

        if (type == null) {
            lastResult = PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT;
            throw new QueryException("Unrecognized statement type: " + firstWord);
        }

        try {
            Statement statement;

            switch (type) {
                case STATEMENT_INSERT:
                    statement = parseInsert(trimmedQuery);
                    break;
                case STATEMENT_SELECT:
                    statement = parseSelect(trimmedQuery);
                    break;
                default:
                    lastResult = PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT;
                    throw new QueryException("Unsupported statement type: " + type);
            }

            lastResult = PrepareResult.PREPARE_SUCCESS;
            logger.debug("Parsed {} statement: {}", type, query);

            return statement;

        } catch (QueryException e) {
            throw e;
        } catch (Exception e) {
            lastResult = PrepareResult.PREPARE_SYNTAX_ERROR;
            throw new QueryException("Syntax error in query: " + e.getMessage(), e);
        }
    }

    @Override
    public PrepareResult getLastResult() {
        return lastResult;
    }

    @Override
    public boolean isValidQuery(String query) {
        try {
            parse(query);
            return true;
        } catch (QueryException e) {
            return false;
        }
    }

    /**
     * Parses an INSERT statement.
     *
     * @param query The INSERT query
     * @return The parsed statement
     * @throws QueryException if parsing fails
     */
    private Statement parseInsert(String query) throws QueryException {
        Matcher matcher = INSERT_PATTERN.matcher(query);

        if (!matcher.matches()) {
            throw new QueryException("Invalid INSERT syntax");
        }

        try {
            int id = Integer.parseInt(matcher.group(1));
            String username = matcher.group(2);
            String email = matcher.group(3);

            if (id <= 0) {
                lastResult = PrepareResult.PREPARE_NEGATIVE_ID;
                throw new QueryException("ID must be positive");
            }

            // Create row - validation happens in Row constructor
            Row row = new Row(id, username, email);

            return Statement.insertStatement(row, query);

        } catch (NumberFormatException e) {
            throw new QueryException("Invalid ID format");
        } catch (Exception e) {
            lastResult = PrepareResult.PREPARE_INVALID_DATA;
            throw new QueryException("Invalid data: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a SELECT statement using improved logic.
     *
     * @param query The SELECT query
     * @return The parsed statement
     * @throws QueryException if parsing fails
     */
    private Statement parseSelect(String query) throws QueryException {
        // Use more robust parsing approach
        String lowerQuery = query.toLowerCase().trim();

        // Find the position of "where" keyword
        int whereIndex = -1;
        String[] parts = lowerQuery.split("\\s+");

        for (int i = 0; i < parts.length; i++) {
            if ("where".equals(parts[i])) {
                whereIndex = i;
                break;
            }
        }

        String columns = null;
        String whereClause = null;

        if (whereIndex == -1) {
            // No WHERE clause
            if (parts.length > 1) {
                // Extract everything after "select" as columns
                int selectEndIndex = query.toLowerCase().indexOf("select") + 6;
                String afterSelect = query.substring(selectEndIndex).trim();
                if (!afterSelect.isEmpty()) {
                    columns = afterSelect;
                }
            }
        } else {
            // Has WHERE clause
            if (whereIndex > 1) {
                // There are columns between SELECT and WHERE
                String[] originalParts = query.trim().split("\\s+");
                StringBuilder columnsBuilder = new StringBuilder();
                for (int i = 1; i < whereIndex; i++) {
                    if (columnsBuilder.length() > 0) {
                        columnsBuilder.append(" ");
                    }
                    columnsBuilder.append(originalParts[i]);
                }
                columns = columnsBuilder.toString();
            }

            // Extract WHERE clause
            String[] originalParts = query.trim().split("\\s+");
            StringBuilder whereBuilder = new StringBuilder();
            for (int i = whereIndex + 1; i < originalParts.length; i++) {
                if (whereBuilder.length() > 0) {
                    whereBuilder.append(" ");
                }
                whereBuilder.append(originalParts[i]);
            }
            whereClause = whereBuilder.toString();
        }

        // Parse columns
        String[] selectColumns = null;
        if (columns != null && !columns.trim().isEmpty() && !columns.equals("*")) {
            selectColumns = columns.split(",");
            for (int i = 0; i < selectColumns.length; i++) {
                selectColumns[i] = selectColumns[i].trim();
            }
        }

        return Statement.selectStatement(whereClause, selectColumns, query);
    }
}