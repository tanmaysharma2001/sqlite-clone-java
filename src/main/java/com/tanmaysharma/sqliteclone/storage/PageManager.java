package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.exception.StorageException;

/**
 * Interface for page management operations.
 * Handles memory paging and disk I/O for database pages.
 */
public interface PageManager {

    /**
     * Gets a page, loading it from disk if necessary.
     *
     * @param pageNum The page number
     * @return The requested page
     * @throws StorageException if the page cannot be loaded
     */
    Page getPage(int pageNum) throws StorageException;

    /**
     * Marks a page as dirty (modified).
     *
     * @param pageNum The page number
     */
    void markPageDirty(int pageNum);

    /**
     * Checks if a page is dirty.
     *
     * @param pageNum The page number
     * @return True if the page is dirty
     */
    boolean isPageDirty(int pageNum);

    /**
     * Flushes a specific page to disk.
     *
     * @param pageNum The page number
     * @throws StorageException if the flush fails
     */
    void flushPage(int pageNum) throws StorageException;

    /**
     * Flushes all dirty pages to disk.
     *
     * @throws StorageException if any flush fails
     */
    void flushAllPages() throws StorageException;

    /**
     * Gets cache statistics.
     *
     * @return Cache statistics string
     */
    String getCacheStatistics();

    /**
     * Closes the page manager and releases resources.
     *
     * @throws StorageException if the close operation fails
     */
    void close() throws StorageException;
}