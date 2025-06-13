package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the page manager.
 * Handles memory paging, caching, and disk I/O operations.
 */
public class PageManagerImpl implements PageManager {

    private static final Logger logger = LoggerFactory.getLogger(PageManagerImpl.class);

    private final String filename;
    private RandomAccessFile file;
    private final ConcurrentHashMap<Integer, Page> pageCache;
    private final BitSet dirtyPages;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private boolean closed = false;

    /**
     * Creates a new page manager for the specified file.
     *
     * @param filename The database file
     * @throws StorageException if initialization fails
     */
    public PageManagerImpl(String filename) throws StorageException {
        this.filename = filename;
        this.pageCache = new ConcurrentHashMap<>();
        this.dirtyPages = new BitSet(DatabaseConfig.MAX_PAGES);

        try {
            this.file = new RandomAccessFile(filename, "rw");
            logger.info("Page manager initialized for file: {}", filename);
        } catch (IOException e) {
            throw new StorageException("Failed to open database file: " + filename, e);
        }
    }

    @Override
    public Page getPage(int pageNum) throws StorageException {
        if (pageNum < 0 || pageNum >= DatabaseConfig.MAX_PAGES) {
            throw new StorageException("Page number out of bounds: " + pageNum);
        }

        lock.readLock().lock();
        try {
            checkNotClosed();

            // Check cache first
            Page page = pageCache.get(pageNum);
            if (page != null) {
                cacheHits.incrementAndGet();
                page.touch();
                return page;
            }

            // Cache miss - load from disk
            cacheMisses.incrementAndGet();

        } finally {
            lock.readLock().unlock();
        }

        // Load page from disk (requires write lock for cache modification)
        lock.writeLock().lock();
        try {
            checkNotClosed();

            // Double-check cache in case another thread loaded it
            Page page = pageCache.get(pageNum);
            if (page != null) {
                page.touch();
                return page;
            }

            // Load from disk
            page = loadPageFromDisk(pageNum);

            // Add to cache
            pageCache.put(pageNum, page);

            logger.debug("Loaded page {} from disk", pageNum);

            return page;

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void markPageDirty(int pageNum) {
        if (pageNum >= 0 && pageNum < DatabaseConfig.MAX_PAGES) {
            dirtyPages.set(pageNum);
        }
    }

    @Override
    public boolean isPageDirty(int pageNum) {
        return pageNum >= 0 && pageNum < DatabaseConfig.MAX_PAGES && dirtyPages.get(pageNum);
    }

    @Override
    public void flushPage(int pageNum) throws StorageException {
        if (!isPageDirty(pageNum)) {
            return; // Nothing to flush
        }

        lock.readLock().lock();
        try {
            checkNotClosed();

            Page page = pageCache.get(pageNum);
            if (page == null) {
                return; // Page not in cache
            }

            writePageToDisk(page);
            dirtyPages.clear(pageNum);

            logger.debug("Flushed page {} to disk", pageNum);

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void flushAllPages() throws StorageException {
        lock.readLock().lock();
        try {
            checkNotClosed();

            for (int pageNum = dirtyPages.nextSetBit(0); pageNum >= 0;
                 pageNum = dirtyPages.nextSetBit(pageNum + 1)) {

                Page page = pageCache.get(pageNum);
                if (page != null) {
                    writePageToDisk(page);
                    dirtyPages.clear(pageNum);
                }
            }

            // Ensure all data is written to disk
            file.getFD().sync();

            logger.debug("Flushed all dirty pages to disk");

        } catch (IOException e) {
            throw new StorageException("Failed to sync file to disk", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getCacheStatistics() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;

        if (total == 0) {
            return "Cache: no operations";
        }

        double hitRatio = (double) hits / total * 100;

        return String.format("Cache: %d hits, %d misses (%.1f%% hit ratio), %d pages cached",
                hits, misses, hitRatio, pageCache.size());
    }

    @Override
    public void close() throws StorageException {
        lock.writeLock().lock();
        try {
            if (closed) {
                return;
            }

            // Flush all dirty pages
            flushAllPages();

            // Close file
            if (file != null) {
                file.close();
            }

            // Clear cache
            pageCache.clear();
            dirtyPages.clear();

            closed = true;

            logger.info("Page manager closed");

        } catch (IOException e) {
            throw new StorageException("Failed to close page manager", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads a page from disk.
     *
     * @param pageNum The page number
     * @return The loaded page
     * @throws StorageException if loading fails
     */
    private Page loadPageFromDisk(int pageNum) throws StorageException {
        try {
            long offset = (long) pageNum * DatabaseConfig.PAGE_SIZE;

            // Check if page exists in file
            if (offset >= file.length()) {
                // Create empty page
                return new Page(pageNum);
            }

            // Read page from disk
            byte[] data = new byte[DatabaseConfig.PAGE_SIZE];
            file.seek(offset);

            int bytesRead = file.read(data);

            if (bytesRead < DatabaseConfig.PAGE_SIZE) {
                // Fill remaining bytes with zeros
                for (int i = bytesRead; i < DatabaseConfig.PAGE_SIZE; i++) {
                    data[i] = 0;
                }
            }

            return new Page(data, pageNum);

        } catch (IOException e) {
            throw new StorageException("Failed to load page " + pageNum + " from disk", e);
        }
    }

    /**
     * Writes a page to disk.
     *
     * @param page The page to write
     * @throws StorageException if writing fails
     */
    private void writePageToDisk(Page page) throws StorageException {
        try {
            long offset = (long) page.getPageNumber() * DatabaseConfig.PAGE_SIZE;

            file.seek(offset);
            file.write(page.getRawData());

        } catch (IOException e) {
            throw new StorageException("Failed to write page " + page.getPageNumber() + " to disk", e);
        }
    }

    /**
     * Checks if the page manager is closed.
     *
     * @throws StorageException if closed
     */
    private void checkNotClosed() throws StorageException {
        if (closed) {
            throw new StorageException("Page manager is closed");
        }
    }
}