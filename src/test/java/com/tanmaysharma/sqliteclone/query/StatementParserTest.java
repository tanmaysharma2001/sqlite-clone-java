package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.PrepareResult;
import com.tanmaysharma.sqliteclone.enums.StatementType;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import com.tanmaysharma.sqliteclone.model.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Statement Parser Tests")
class StatementParserTest {

    private StatementParser parser;

    @BeforeEach
    void setUp() {
        parser = new StatementParserImpl();
    }

    @Nested
    @DisplayName("INSERT Statement Parsing")
    class InsertStatementParsing {

        @Test
        @DisplayName("Should parse valid INSERT statement")
        void shouldParseValidInsertStatement() throws QueryException {
            String query = "insert 1 testuser test@example.com";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_INSERT, statement.getType());
            assertNotNull(statement.getRowToInsert());
            assertEquals(1, statement.getRowToInsert().getId());
            assertEquals("testuser", statement.getRowToInsert().getUsername());
            assertEquals("test@example.com", statement.getRowToInsert().getEmail());
            assertEquals(PrepareResult.PREPARE_SUCCESS, parser.getLastResult());
        }

        @Test
        @DisplayName("Should parse INSERT with case insensitive keyword")
        void shouldParseInsertWithCaseInsensitiveKeyword() throws QueryException {
            String query = "INSERT 2 user2 user2@example.com";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_INSERT, statement.getType());
            assertEquals(2, statement.getRowToInsert().getId());
        }

        @Test
        @DisplayName("Should reject INSERT with negative ID")
        void shouldRejectInsertWithNegativeId() {
            String query = "insert -1 testuser test@example.com";

            QueryException exception = assertThrows(QueryException.class, () -> parser.parse(query));
            assertTrue(exception.getMessage().contains("positive") ||
                    exception.getMessage().contains("ID must be positive"));
        }

        @Test
        @DisplayName("Should reject INSERT with invalid format")
        void shouldRejectInsertWithInvalidFormat() {
            String query = "insert 1 testuser"; // Missing email

            assertThrows(QueryException.class, () -> parser.parse(query));
        }

        @Test
        @DisplayName("Should reject INSERT with non-numeric ID")
        void shouldRejectInsertWithNonNumericId() {
            String query = "insert abc testuser test@example.com";

            assertThrows(QueryException.class, () -> parser.parse(query));
        }
    }

    @Nested
    @DisplayName("SELECT Statement Parsing")
    class SelectStatementParsing {

        @Test
        @DisplayName("Should parse simple SELECT statement")
        void shouldParseSimpleSelectStatement() throws QueryException {
            String query = "select";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_SELECT, statement.getType());
            assertFalse(statement.hasWhereClause());
            assertFalse(statement.hasSpecificColumns());
            assertEquals(PrepareResult.PREPARE_SUCCESS, parser.getLastResult());
        }

        @Test
        @DisplayName("Should parse SELECT with WHERE clause")
        void shouldParseSelectWithWhereClause() throws QueryException {
            String query = "select where id = 1";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_SELECT, statement.getType());
            assertTrue(statement.hasWhereClause());
            assertEquals("id = 1", statement.getWhereClause());
            assertFalse(statement.hasSpecificColumns()); // No specific columns, just WHERE
        }

        @Test
        @DisplayName("Should parse SELECT with case insensitive keyword")
        void shouldParseSelectWithCaseInsensitiveKeyword() throws QueryException {
            String query = "SELECT";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_SELECT, statement.getType());
        }

        @Test
        @DisplayName("Should parse SELECT with wildcard")
        void shouldParseSelectWithWildcard() throws QueryException {
            String query = "select *";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_SELECT, statement.getType());
            assertFalse(statement.hasSpecificColumns()); // * means no specific columns
        }

        @Test
        @DisplayName("Should parse SELECT with columns and WHERE clause")
        void shouldParseSelectWithColumnsAndWhereClause() throws QueryException {
            String query = "select id, username where id = 1";
            Statement statement = parser.parse(query);

            assertEquals(StatementType.STATEMENT_SELECT, statement.getType());
            assertTrue(statement.hasWhereClause());
            assertEquals("id = 1", statement.getWhereClause());
            assertTrue(statement.hasSpecificColumns());
            assertArrayEquals(new String[]{"id", "username"}, statement.getSelectColumns());
        }
    }

    @Nested
    @DisplayName("Invalid Statement Parsing")
    class InvalidStatementParsing {

        @Test
        @DisplayName("Should reject null query")
        void shouldRejectNullQuery() {
            QueryException exception = assertThrows(QueryException.class, () -> parser.parse(null));
            assertTrue(exception.getMessage().contains("null") ||
                    exception.getMessage().contains("empty"));
        }

        @Test
        @DisplayName("Should reject empty query")
        void shouldRejectEmptyQuery() {
            QueryException exception = assertThrows(QueryException.class, () -> parser.parse(""));
            assertTrue(exception.getMessage().contains("empty") ||
                    exception.getMessage().contains("null"));
        }

        @Test
        @DisplayName("Should reject unrecognized statement type")
        void shouldRejectUnrecognizedStatementType() {
            String query = "update table set value = 1";

            QueryException exception = assertThrows(QueryException.class, () -> parser.parse(query));
            assertTrue(exception.getMessage().contains("Unrecognized") ||
                    exception.getMessage().contains("Unsupported"));
        }

        @Test
        @DisplayName("Should reject query that exceeds maximum length")
        void shouldRejectQueryThatExceedsMaximumLength() {
            String longQuery = "insert 1 " + "a".repeat(2000) + " test@example.com";

            QueryException exception = assertThrows(QueryException.class, () -> parser.parse(longQuery));
            assertTrue(exception.getMessage().contains("length"));
        }
    }

    @Nested
    @DisplayName("Query Validation")
    class QueryValidation {

        @Test
        @DisplayName("Should validate correct queries")
        void shouldValidateCorrectQueries() {
            assertTrue(parser.isValidQuery("insert 1 test test@example.com"));
            assertTrue(parser.isValidQuery("select"));
            assertTrue(parser.isValidQuery("select where id = 1"));
        }

        @Test
        @DisplayName("Should invalidate incorrect queries")
        void shouldInvalidateIncorrectQueries() {
            assertFalse(parser.isValidQuery("invalid query"));
            assertFalse(parser.isValidQuery(""));
            assertFalse(parser.isValidQuery(null));
        }
    }
}