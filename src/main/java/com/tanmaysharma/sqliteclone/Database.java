package com.tanmaysharma.sqliteclone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages database operations and provides constants for database configuration.
 * Implements a connection pool pattern for database access.
 */
public class Database {
    // Database constants
    public static final int ROW_SIZE = 292;  // 4 bytes for ID + 32 bytes for username + 256 bytes for email
    public static final int TABLE_MAX_ROWS = 1000;
    public static final int PAGE_SIZE = 4096;  // 4 KB page size
    public static final int MAX_PAGES = 100;
    public static final int ROWS_PER_PAGE = PAGE_SIZE / ROW_SIZE;  // Number of rows that fit in a page
    
    // Connection pool
    private static final Map<String, Table> openConnections = new ConcurrentHashMap<>();
    
    // Cache statistics
    private static int cacheHits = 0;
    private static int cacheMisses = 0;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private Database() {
        // Utility class - do not instantiate
    }
    
    /**
     * Opens a database file and returns a table reference.
     * Uses connection pooling to avoid reopening the same file.
     *
     * @param filename The database file to open
     * @return A table for the database
     */
    public static Table open(String filename) {
        // Check if we already have this database open
        return openConnections.computeIfAbsent(filename, key -> {
            Pager pager = new Pager(key);
            Table table = new Table(pager);
            
            // Add shutdown hook to ensure proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                close(table);
            }));
            
            return table;
        });
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
        
        Pager pager = table.getPager();
        if (pager == null) {
            return false;
        }
        
        try {
            // Remove from connection pool
            openConnections.values().removeIf(t -> t == table);
            
            // Flush all dirty pages to disk
            int numFullPages = table.getNumRows() / ROWS_PER_PAGE;
            boolean success = true;
            
            // Write any partial pages
            int numAdditionalRows = table.getNumRows() % ROWS_PER_PAGE;
            if (numAdditionalRows > 0) {
                int pageNum = numFullPages;
                if (pager.getPages()[pageNum] != null && !pager.flush(pageNum, numAdditionalRows * ROW_SIZE)) {
                    success = false;
                }
            }
            
            // Close the pager
            if (!pager.close()) {
                success = false;
            }
            
            if (success) {
                System.out.println("Database closed successfully.");
                printCacheStatistics();
            } else {
                System.err.println("Warning: Some issues occurred while closing database.");
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error closing database: " + e.getMessage());
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
                System.out.printf("Cache statistics: %d hits, %d misses (%.1f%% hit ratio)%n", 
                                 cacheHits, cacheMisses, hitRatio);
            } else {
                System.out.println("Cache statistics: No cache operations performed");
            }
        } catch (Exception e) {
            // Fail silently - statistics are non-critical
            System.out.println("Cache statistics: unavailable");
        }
    }

    /**
     * Reads a page from disk if not already in memory.
     *
     * @param pager The pager to use
     * @param pageNum The page number to read
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
                System.err.println("Error reading page " + pageNum + ": " + e.getMessage());
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
            System.err.println("Tried to write page number out of bounds: " + pageNum);
            return false;
        }

        if (pager.getPages()[pageNum] != null) {
            return pager.flush(pageNum, PAGE_SIZE);
        }
        
        return true; // Nothing to write
    }
    
    /**
     * Closes all open database connections.
     */
    public static void closeAll() {
        for (Table table : new HashMap<>(openConnections).values()) {
            close(table);
        }
        openConnections.clear();
    }
}