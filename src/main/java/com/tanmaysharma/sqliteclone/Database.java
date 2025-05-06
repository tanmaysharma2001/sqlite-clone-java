package com.tanmaysharma.sqliteclone;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database operations and provides constants for database configuration.
 * Implements a connection pool pattern for database access with proper resource limits.
 */
public class Database {
    // Database constants
    public static final int ROW_SIZE = 292;  // 4 bytes for ID + 32 bytes for username + 256 bytes for email
    public static final int TABLE_MAX_ROWS = 1000;
    public static final int PAGE_SIZE = 4096;  // 4 KB page size
    public static final int MAX_PAGES = 100;
    public static final int ROWS_PER_PAGE = PAGE_SIZE / ROW_SIZE;  // Number of rows that fit in a page
    
    // Connection pool with limits
    private static final int MAX_CONNECTIONS = 10;
    private static final Map<String, Table> openConnections = new ConcurrentHashMap<>(MAX_CONNECTIONS);
    private static final Semaphore connectionLimiter = new Semaphore(MAX_CONNECTIONS);
    
    // Cache statistics with thread-safe counters
    private static volatile int cacheHits = 0;
    private static volatile int cacheMisses = 0;
    
    // Logger instead of System.out/err
    private static final Logger logger = Logger.getLogger(Database.class.getName());
    
    // Add connection timeout
    private static final long CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    
    /**
     * Private constructor to prevent instantiation.
     */
    private Database() {
        // Utility class - do not instantiate
    }
    
    /**
     * Opens a database file and returns a table reference.
     * Uses connection pooling with limits to avoid reopening the same file.
     *
     * @param filename The database file to open
     * @return A table for the database
     * @throws DatabaseException if the database cannot be opened
     */
    public static Table open(String filename) {
        validateFilename(filename);
        
        try {
            // Try to acquire a connection slot with timeout
            if (!connectionLimiter.tryAcquire(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new DatabaseException("Connection limit reached, timeout after " + 
                                           CONNECTION_TIMEOUT_MS + "ms");
            }
            
            try {
                // Check if we already have this database open
                return openConnections.computeIfAbsent(filename, key -> {
                    logger.info("Opening database: " + key);
                    try {
                        Pager pager = new Pager(key);
                        Table table = new Table(pager);
                        return table;
                    } catch (Exception e) {
                        // Release the connection semaphore on failure
                        connectionLimiter.release();
                        throw new DatabaseException("Failed to initialize database: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                // Release the connection on error
                connectionLimiter.release();
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Interrupted while waiting for database connection", e);
        }
    }
    
    /**
     * Validates database filename before opening.
     * 
     * @param filename The filename to validate
     * @throws IllegalArgumentException if the filename is invalid
     */
    private static void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Database filename cannot be empty");
        }
        
        File file = new File(filename);
        File parentDir = file.getParentFile();
        
        // Ensure parent directory exists or can be created
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory: " + parentDir);
            }
        }
        
        // Check if file can be created or is writable
        if (file.exists() && !file.canWrite()) {
            throw new IllegalArgumentException("Cannot write to file: " + filename);
        }
    }

    /**
     * Closes a database connection and performs cleanup.
     *
     * @param table The table to close
     * @return True if closed successfully, false otherwise
     */
    public static boolean close(Table table) {
        if (table == null) {
            return false;
        }
        
        try {
            Pager pager = table.getPager();
            if (pager == null) {
                return false;
            }
            
            // Remove from connection pool and release semaphore
            boolean removed = openConnections.values().removeIf(t -> t == table);
            
            if (removed) {
                connectionLimiter.release();
            }
            
            // Flush pages to disk
            boolean success = true;
            
            try {
                // Flush all dirty pages
                if (!flushPages(table)) {
                    success = false;
                }
                
                // Close the pager
                if (!pager.close()) {
                    success = false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during close operation", e);
                success = false;
            }
            
            if (success) {
                logger.info("Database closed successfully.");
                printCacheStatistics();
            } else {
                logger.warning("Some issues occurred while closing database.");
            }
            
            return success;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing database", e);
            return false;
        }
    }
    
    /**
     * Flush all dirty pages to disk.
     * 
     * @param table The table to flush
     * @return True if all pages were flushed successfully
     */
    private static boolean flushPages(Table table) {
        try {
            Pager pager = table.getPager();
            if (pager == null) {
                return false;
            }
            
            int numFullPages = table.getNumRows() / ROWS_PER_PAGE;
            boolean success = true;
            
            // Flush all full pages
            for (int i = 0; i < numFullPages; i++) {
                if (pager.isPageDirty(i) && !pager.flush(i, PAGE_SIZE)) {
                    success = false;
                }
            }
            
            // Write any partial page
            int numAdditionalRows = table.getNumRows() % ROWS_PER_PAGE;
            if (numAdditionalRows > 0) {
                int pageNum = numFullPages;
                if (pager.getPages()[pageNum] != null && pager.isPageDirty(pageNum) && 
                    !pager.flush(pageNum, numAdditionalRows * ROW_SIZE)) {
                    success = false;
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error flushing pages", e);
            return false;
        }
    }
    
    /**
     * Prints cache hit/miss statistics.
     */
    private static void printCacheStatistics() {
        try {
            int total = cacheHits + cacheMisses;
            if (total > 0) {
                double hitRatio = (double) cacheHits / total * 100;
                logger.info(String.format("Cache statistics: %d hits, %d misses (%.1f%% hit ratio)", 
                                        cacheHits, cacheMisses, hitRatio));
            } else {
                logger.info("Cache statistics: No cache operations performed");
            }
        } catch (Exception e) {
            // Fail silently - statistics are non-critical
            logger.info("Cache statistics: unavailable");
        }
    }

    /**
     * Reads a page from disk if not already in memory.
     *
     * @param pager The pager to use
     * @param pageNum The page number to read
     * @throws DatabaseException if the page cannot be read
     */
    public static void readPage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            throw new IllegalArgumentException("Page number out of bounds: " + pageNum);
        }

        if (pager.getPages()[pageNum] == null) {
            // Page cache miss, load from disk
            cacheMisses++;
            try {
                pager.readPageFromDisk(pageNum);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading page " + pageNum, e);
                // Initialize empty page if read fails
                pager.getPages()[pageNum] = new Page(new byte[PAGE_SIZE]);
            }
        } else {
            // Page cache hit
            cacheHits++;
        }
    }

    /**
     * Writes a page to disk.
     *
     * @param pager The pager to use
     * @param pageNum The page number to write
     * @return True if the page was written successfully, false otherwise
     */
    public static boolean writePage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            logger.warning("Tried to write page number out of bounds: " + pageNum);
            return false;
        }

        if (pager.getPages()[pageNum] != null && pager.isPageDirty(pageNum)) {
            return pager.flush(pageNum, PAGE_SIZE);
        }
        
        return true; // Nothing to write
    }
    
    /**
     * Closes all open database connections.
     */
    public static void closeAll() {
        for (Table table : new HashMap<>(openConnections).values()) {
            try {
                close(table);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing database", e);
            }
        }
        openConnections.clear();
        logger.info("All database connections closed");
    }
    
    /**
     * Gets the current connection count.
     * 
     * @return The number of open connections
     */
    public static int getConnectionCount() {
        return openConnections.size();
    }
    
    /**
     * Gets connection statistics.
     * 
     * @return A string with connection statistics
     */
    public static String getConnectionStats() {
        return String.format("Connections: %d/%d (available: %d)", 
                         openConnections.size(), MAX_CONNECTIONS, 
                         connectionLimiter.availablePermits());
    }
    
    /**
     * Resets cache statistics.
     */
    public static void resetCacheStatistics() {
        cacheHits = 0;
        cacheMisses = 0;
    }
}