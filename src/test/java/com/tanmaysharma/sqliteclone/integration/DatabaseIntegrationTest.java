package com.tanmaysharma.sqliteclone.integration;

import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.query.QueryEngine;
import com.tanmaysharma.sqliteclone.query.QueryEngineImpl;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import com.tanmaysharma.sqliteclone.storage.StorageEngineImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private QueryEngine queryEngine;
    private Path testDbFile;

    @BeforeEach
    void setUp() throws Exception {
        testDbFile = tempDir.resolve("integration_test.db");
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
    @DisplayName("Should handle complete workflow")
    void shouldHandleCompleteWorkflow() throws Exception {
        // Insert multiple rows
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 1 alice alice@example.com"));
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 2 bob bob@example.com"));
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 3 charlie charlie@example.com"));

        // Verify row count
        assertEquals(3, storageEngine.getRowCount());

        // Test individual lookups
        assertNotNull(storageEngine.getRowById(1));
        assertNotNull(storageEngine.getRowById(2));
        assertNotNull(storageEngine.getRowById(3));
        assertNull(storageEngine.getRowById(4));

        // Test SELECT operations
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("select"));
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("select where id = 2"));

        // Test duplicate insertion
        assertEquals(ExecuteResult.EXECUTE_DUPLICATE_KEY,
                queryEngine.executeQuery("insert 1 duplicate duplicate@example.com"));
    }

    @Test
    @DisplayName("Should persist data across restarts")
    void shouldPersistDataAcrossRestarts() throws Exception {
        // Insert data in first session
        queryEngine.executeQuery("insert 1 alice alice@example.com");
        queryEngine.executeQuery("insert 2 bob bob@example.com");

        // Close storage
        storageEngine.close();

        // Reopen storage
        storageEngine = new StorageEngineImpl();
        storageEngine.initialize(testDbFile.toString());
        queryEngine = new QueryEngineImpl(storageEngine);

        // Verify data persists
        assertEquals(2, storageEngine.getRowCount());
        assertNotNull(storageEngine.getRowById(1));
        assertNotNull(storageEngine.getRowById(2));

        // Should be able to insert new data
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 3 charlie charlie@example.com"));

        assertEquals(3, storageEngine.getRowCount());
    }

    @Test
    @DisplayName("Should handle edge cases")
    void shouldHandleEdgeCases() throws Exception {
        // Test with maximum length strings
        String longUsername = "a".repeat(32);
        String longEmail = "a".repeat(200) + "@example.com";

        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery(String.format("insert 1 %s %s", longUsername, longEmail)));

        // Test with special characters
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 2 user@name user+test@example.com"));

        // Test boundary conditions
        assertEquals(ExecuteResult.EXECUTE_SUCCESS,
                queryEngine.executeQuery("insert 2147483647 maxuser max@example.com"));
    }
}