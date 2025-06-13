package com.tanmaysharma.sqliteclone.query;

import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import com.tanmaysharma.sqliteclone.storage.StorageEngineImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Query Engine Tests")
class QueryEngineTest {

    @TempDir
    Path tempDir;

    private QueryEngine queryEngine;
    private StorageEngine storageEngine;
    private Path testDbFile;

    @BeforeEach
    void setUp() throws Exception {
        testDbFile = tempDir.resolve("test.db");
        storageEngine = new StorageEngineImpl();
        storageEngine.initialize(testDbFile.toString());
        queryEngine = new QueryEngineImpl(storageEngine);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (storageEngine != null) {
            storageEngine.close();
        }
    }

    @Test
    @DisplayName("Should execute INSERT query")
    void shouldExecuteInsertQuery() throws QueryException {
        String query = "insert 1 testuser test@example.com";
        ExecuteResult result = queryEngine.executeQuery(query);

        assertEquals(ExecuteResult.EXECUTE_SUCCESS, result);
        assertEquals(1, storageEngine.getRowCount());
        assertTrue(storageEngine.rowExists(1));
    }

    @Test
    @DisplayName("Should execute SELECT query")
    void shouldExecuteSelectQuery() throws QueryException {
        // First insert some data
        queryEngine.executeQuery("insert 1 user1 user1@example.com");
        queryEngine.executeQuery("insert 2 user2 user2@example.com");

        // Then select all
        ExecuteResult result = queryEngine.executeQuery("select");
        assertEquals(ExecuteResult.EXECUTE_SUCCESS, result);
    }

    @Test
    @DisplayName("Should execute SELECT with WHERE clause")
    void shouldExecuteSelectWithWhereClause() throws QueryException {
        // Insert data
        queryEngine.executeQuery("insert 1 user1 user1@example.com");
        queryEngine.executeQuery("insert 2 user2 user2@example.com");

        // Select specific row - this should now work with the fixed parser and validator
        ExecuteResult result = queryEngine.executeQuery("select where id = 1");
        assertEquals(ExecuteResult.EXECUTE_SUCCESS, result);
    }

    @Test
    @DisplayName("Should reject invalid syntax")
    void shouldRejectInvalidSyntax() {
        assertThrows(QueryException.class, () ->
                queryEngine.executeQuery("invalid query"));
    }

    @Test
    @DisplayName("Should reject null query")
    void shouldRejectNullQuery() {
        assertThrows(QueryException.class, () ->
                queryEngine.executeQuery(null));
    }

    @Test
    @DisplayName("Should reject empty query")
    void shouldRejectEmptyQuery() {
        assertThrows(QueryException.class, () ->
                queryEngine.executeQuery(""));
    }

    @Test
    @DisplayName("Should handle duplicate key insertion")
    void shouldHandleDuplicateKeyInsertion() throws QueryException {
        queryEngine.executeQuery("insert 1 user1 user1@example.com");

        ExecuteResult result = queryEngine.executeQuery("insert 1 user2 user2@example.com");
        assertEquals(ExecuteResult.EXECUTE_DUPLICATE_KEY, result);
    }

    @Test
    @DisplayName("Should handle invalid WHERE clause format")
    void shouldHandleInvalidWhereClauseFormat() {
        // Insert some data first
        assertDoesNotThrow(() -> queryEngine.executeQuery("insert 1 user1 user1@example.com"));

        // Try invalid WHERE clause format - should throw exception during validation
        assertThrows(QueryException.class, () ->
                queryEngine.executeQuery("select where name = 'john'"));
    }

    @Test
    @DisplayName("Should handle SELECT with specific columns")
    void shouldHandleSelectWithSpecificColumns() throws QueryException {
        // Insert some data first
        queryEngine.executeQuery("insert 1 user1 user1@example.com");

        // Select with specific columns
        ExecuteResult result = queryEngine.executeQuery("select id, username");
        assertEquals(ExecuteResult.EXECUTE_SUCCESS, result);
    }

    @Test
    @DisplayName("Should reject unsupported column names")
    void shouldRejectUnsupportedColumnNames() {
        // Insert some data first
        assertDoesNotThrow(() -> queryEngine.executeQuery("insert 1 user1 user1@example.com"));

        // Try to select unsupported column
        assertThrows(QueryException.class, () ->
                queryEngine.executeQuery("select nonexistent_column"));
    }
}