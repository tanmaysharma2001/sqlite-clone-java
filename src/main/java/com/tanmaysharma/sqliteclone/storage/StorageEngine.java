package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.exception.StorageException;
import com.tanmaysharma.sqliteclone.model.Row;

import java.util.List;

/**
 * Interface for the storage engine.
 * Handles all low-level storage operations and data persistence.
 */
public interface StorageEngine {

    /**
     * Initializes the storage engine with the specified database file.
     *
     * @param filename The database file to use
     * @throws StorageException if initialization fails
     */
    void initialize(String filename) throws StorageException;

    /**
     * Inserts a row into the table.
     *
     * @param row The row to insert
     * @throws StorageException if the insertion fails
     */
    void insertRow(Row row) throws StorageException;

    /**
     * Retrieves all rows from the table.
     *
     * @return List of all rows
     * @throws StorageException if the retrieval fails
     */
    List<Row> getAllRows() throws StorageException;

    /**
     * Retrieves a row by its ID.
     *
     * @param id The row ID
     * @return The row with the specified ID, or null if not found
     * @throws StorageException if the retrieval fails
     */
    Row getRowById(int id) throws StorageException;

    /**
     * Gets the current number of rows in the table.
     *
     * @return The number of rows
     */
    int getRowCount();

    /**
     * Gets the maximum number of rows the table can hold.
     *
     * @return The maximum row count
     */
    int getMaxRows();

    /**
     * Checks if the table is full.
     *
     * @return True if the table is full
     */
    boolean isTableFull();

    /**
     * Checks if a row with the specified ID exists.
     *
     * @param id The row ID to check
     * @return True if the row exists
     */
    boolean rowExists(int id);

    /**
     * Flushes all pending changes to disk.
     *
     * @throws StorageException if the flush operation fails
     */
    void flush() throws StorageException;

    /**
     * Gets storage statistics.
     *
     * @return A string containing storage statistics
     */
    String getStatistics();

    /**
     * Closes the storage engine and releases all resources.
     *
     * @throws StorageException if the close operation fails
     */
    void close() throws StorageException;
}