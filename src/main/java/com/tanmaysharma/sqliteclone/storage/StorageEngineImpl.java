// StorageEngineImpl.java
package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.StorageException;
import com.tanmaysharma.sqliteclone.model.Row;
import com.tanmaysharma.sqliteclone.util.SerializationUtil;
import com.tanmaysharma.sqliteclone.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the storage engine.
 * Handles row storage, indexing, and retrieval operations.
 */
public class StorageEngineImpl implements StorageEngine {

    private static final Logger logger = LoggerFactory.getLogger(StorageEngineImpl.class);

    private PageManager pageManager;
    private volatile int rowCount;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Integer, Integer> idToRowIndex = new ConcurrentHashMap<>();
    private String filename;
    private boolean initialized = false;

    @Override
    public void initialize(String filename) throws StorageException {
        lock.writeLock().lock();
        try {
            if (initialized) {
                throw new StorageException("Storage engine already initialized");
            }

            ValidationUtil.validateDatabaseFile(filename);
            this.filename = filename;

            // Initialize page manager
            this.pageManager = new PageManagerImpl(filename);

            // Count existing rows and build index
            this.rowCount = scanAndBuildIndex();

            this.initialized = true;

            logger.info("Storage engine initialized with {} rows", rowCount);

        } catch (Exception e) {
            throw new StorageException("Failed to initialize storage engine", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void insertRow(Row row) throws StorageException {
        ValidationUtil.requireNonNull(row, "row");

        lock.writeLock().lock();
        try {
            checkInitialized();

            if (isTableFull()) {
                throw new StorageException("Table is full");
            }

            if (rowExists(row.getId())) {
                throw new StorageException("Row with ID " + row.getId() + " already exists");
            }

            // Calculate where to store the row
            int pageNum = rowCount / DatabaseConfig.ROWS_PER_PAGE;
            int rowOffset = rowCount % DatabaseConfig.ROWS_PER_PAGE;

            // Get the page
            Page page = pageManager.getPage(pageNum);

            // Serialize the row to the page
            byte[] rowData = SerializationUtil.serializeRow(row);
            int byteOffset = rowOffset * DatabaseConfig.ROW_SIZE;

            page.writeBytes(rowData, byteOffset);

            // Mark page as dirty
            pageManager.markPageDirty(pageNum);

            // Update index and count
            idToRowIndex.put(row.getId(), rowCount);
            rowCount++;

            logger.debug("Inserted row with ID {} at position {}", row.getId(), rowCount - 1);

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Row> getAllRows() throws StorageException {
        lock.readLock().lock();
        try {
            checkInitialized();

            List<Row> rows = new ArrayList<>();

            for (int i = 0; i < rowCount; i++) {
                Row row = getRowAtIndex(i);
                if (row != null) {
                    rows.add(row);
                }
            }

            return rows;

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Row getRowById(int id) throws StorageException {
        lock.readLock().lock();
        try {
            checkInitialized();

            Integer rowIndex = idToRowIndex.get(id);
            if (rowIndex == null) {
                return null;
            }

            return getRowAtIndex(rowIndex);

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getMaxRows() {
        return DatabaseConfig.TABLE_MAX_ROWS;
    }

    @Override
    public boolean isTableFull() {
        return rowCount >= DatabaseConfig.TABLE_MAX_ROWS;
    }

    @Override
    public boolean rowExists(int id) {
        return idToRowIndex.containsKey(id);
    }

    @Override
    public void flush() throws StorageException {
        lock.readLock().lock();
        try {
            checkInitialized();
            pageManager.flushAllPages();
            logger.debug("Flushed all pages to disk");
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getStatistics() {
        lock.readLock().lock();
        try {
            if (!initialized) {
                return "Storage engine not initialized";
            }

            return String.format(
                    "Storage Statistics:\n" +
                            "  Rows: %d/%d\n" +
                            "  Pages in use: %d\n" +
                            "  Index entries: %d\n" +
                            "  %s",
                    rowCount, getMaxRows(),
                    (rowCount + DatabaseConfig.ROWS_PER_PAGE - 1) / DatabaseConfig.ROWS_PER_PAGE,
                    idToRowIndex.size(),
                    pageManager.getCacheStatistics()
            );

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws StorageException {
        lock.writeLock().lock();
        try {
            if (!initialized) {
                return;
            }

            // Flush all changes
            flush();

            // Close page manager
            if (pageManager != null) {
                pageManager.close();
            }

            // Clear index
            idToRowIndex.clear();

            initialized = false;

            logger.info("Storage engine closed");

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Scans the database file and builds the ID index.
     *
     * @return The number of rows found
     * @throws StorageException if scanning fails
     */
    private int scanAndBuildIndex() throws StorageException {
        int count = 0;
        idToRowIndex.clear();

        try {
            // Calculate maximum possible rows based on configuration
            int maxPossibleRows = Math.min(
                    DatabaseConfig.TABLE_MAX_ROWS,
                    DatabaseConfig.MAX_PAGES * DatabaseConfig.ROWS_PER_PAGE
            );

            for (int i = 0; i < maxPossibleRows; i++) {
                Row row = getRowAtIndex(i);
                if (row != null) {
                    idToRowIndex.put(row.getId(), i);
                    count = i + 1; // Update count to include this row
                } else {
                    // If we hit an empty row, we've reached the end
                    break;
                }
            }

            logger.debug("Scanned database and found {} rows", count);

        } catch (Exception e) {
            throw new StorageException("Failed to scan database and build index", e);
        }

        return count;
    }

    /**
     * Gets a row at the specified index.
     *
     * @param index The row index
     * @return The row, or null if empty
     * @throws StorageException if retrieval fails
     */
    private Row getRowAtIndex(int index) throws StorageException {
        if (index < 0 || index >= DatabaseConfig.TABLE_MAX_ROWS) {
            return null;
        }

        try {
            int pageNum = index / DatabaseConfig.ROWS_PER_PAGE;
            int rowOffset = index % DatabaseConfig.ROWS_PER_PAGE;

            Page page = pageManager.getPage(pageNum);
            int byteOffset = rowOffset * DatabaseConfig.ROW_SIZE;

            byte[] rowData = page.readBytes(byteOffset, DatabaseConfig.ROW_SIZE);

            return SerializationUtil.deserializeRow(rowData, 0);

        } catch (Exception e) {
            throw new StorageException("Failed to get row at index " + index, e);
        }
    }

    /**
     * Checks if the storage engine is initialized.
     *
     * @throws StorageException if not initialized
     */
    private void checkInitialized() throws StorageException {
        if (!initialized) {
            throw new StorageException("Storage engine not initialized");
        }
    }
}