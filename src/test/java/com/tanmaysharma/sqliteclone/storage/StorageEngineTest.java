package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.exception.StorageException;
import com.tanmaysharma.sqliteclone.model.Row;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Storage Engine Tests")
class StorageEngineTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private Path testDbFile;

    @BeforeEach
    void setUp() throws IOException {
        testDbFile = tempDir.resolve("test.db");
        storageEngine = new StorageEngineImpl();
    }

    @AfterEach
    void tearDown() throws StorageException {
        if (storageEngine != null) {
            storageEngine.close();
        }
    }

    @Test
    @DisplayName("Should initialize with new database file")
    void shouldInitializeWithNewDatabaseFile() {
        assertDoesNotThrow(() -> storageEngine.initialize(testDbFile.toString()));
        assertEquals(0, storageEngine.getRowCount());
        assertFalse(storageEngine.isTableFull());
    }

    @Test
    @DisplayName("Should insert and retrieve row")
    void shouldInsertAndRetrieveRow() throws StorageException {
        storageEngine.initialize(testDbFile.toString());

        Row row = new Row(1, "testuser", "test@example.com");
        storageEngine.insertRow(row);

        assertEquals(1, storageEngine.getRowCount());
        assertTrue(storageEngine.rowExists(1));

        Row retrieved = storageEngine.getRowById(1);
        assertEquals(row, retrieved);
    }

    @Test
    @DisplayName("Should prevent duplicate IDs")
    void shouldPreventDuplicateIds() throws StorageException {
        storageEngine.initialize(testDbFile.toString());

        Row row1 = new Row(1, "user1", "user1@example.com");
        Row row2 = new Row(1, "user2", "user2@example.com");

        storageEngine.insertRow(row1);

        assertThrows(StorageException.class, () -> storageEngine.insertRow(row2));
    }

    @Test
    @DisplayName("Should retrieve all rows")
    void shouldRetrieveAllRows() throws StorageException {
        storageEngine.initialize(testDbFile.toString());

        Row row1 = new Row(1, "user1", "user1@example.com");
        Row row2 = new Row(2, "user2", "user2@example.com");

        storageEngine.insertRow(row1);
        storageEngine.insertRow(row2);

        List<Row> allRows = storageEngine.getAllRows();
        assertEquals(2, allRows.size());
        assertTrue(allRows.contains(row1));
        assertTrue(allRows.contains(row2));
    }

    @Test
    @DisplayName("Should persist data across sessions")
    void shouldPersistDataAcrossSessions() throws StorageException {
        // First session - insert data
        storageEngine.initialize(testDbFile.toString());
        Row row = new Row(1, "testuser", "test@example.com");
        storageEngine.insertRow(row);
        storageEngine.close();

        // Second session - verify data persists
        StorageEngine newStorageEngine = new StorageEngineImpl();
        newStorageEngine.initialize(testDbFile.toString());

        assertEquals(1, newStorageEngine.getRowCount());
        Row retrieved = newStorageEngine.getRowById(1);
        assertEquals(row, retrieved);

        newStorageEngine.close();
    }
}