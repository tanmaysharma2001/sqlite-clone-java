package com.tanmaysharma.sqliteclone;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Manages memory paging for database file operations.
 * Provides efficient read/write access to database pages.
 */
public class Pager {
    private RandomAccessFile file;
    private FileChannel channel;
    private Page[] pages;
    private BitSet dirtyPages;
    private int fileLength;
    private String filename;
    
    /**
     * Creates a new pager for the specified database file.
     *
     * @param filename The database file to use
     */
    public Pager(String filename) {
        this.filename = filename;
        try {
            File dbFile = new File(filename);
            boolean fileExists = dbFile.exists() && dbFile.length() > 0;
            
            // Create parent directories if they don't exist
            if (!fileExists && dbFile.getParentFile() != null) {
                dbFile.getParentFile().mkdirs();
            }
            
            // Open the file for read/write with synchronous mode
            this.file = new RandomAccessFile(filename, "rwd");
            this.channel = file.getChannel();
            this.fileLength = (int) file.length();
            this.pages = new Page[Database.MAX_PAGES];
            this.dirtyPages = new BitSet(Database.MAX_PAGES);
            
            System.out.println("Opened database file: " + filename + 
                              (fileExists ? " (existing file)" : " (new file)"));
            
        } catch (IOException e) {
            System.err.println("Error opening database file: " + filename);
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the database file.
     *
     * @return The database file
     */
    public RandomAccessFile getFile() {
        return file;
    }

    /**
     * Gets the array of cached pages.
     *
     * @return The pages array
     */
    public Page[] getPages() {
        return pages;
    }

    /**
     * Gets the current file length.
     *
     * @return The file length in bytes
     */
    public int getFileLength() {
        updateFileLength();
        return fileLength;
    }
    
    /**
     * Updates the cached file length from the actual file.
     */
    public void updateFileLength() {
        try {
            this.fileLength = (int) file.length();
        } catch (IOException e) {
            System.err.println("Error getting file length: " + e.getMessage());
        }
    }

    /**
     * Marks a page as dirty, indicating it needs to be written to disk.
     *
     * @param pageNum The page number to mark
     */
    public void markPageDirty(int pageNum) {
        if (pageNum < Database.MAX_PAGES) {
            dirtyPages.set(pageNum);
        }
    }
    
    /**
     * Checks if a page is marked as dirty.
     *
     * @param pageNum The page number to check
     * @return True if the page is dirty, false otherwise
     */
    public boolean isPageDirty(int pageNum) {
        return pageNum < Database.MAX_PAGES && dirtyPages.get(pageNum);
    }

    /**
     * Flushes a page to disk if it's dirty.
     *
     * @param pageNum The page number to flush
     * @return True if the page was flushed successfully, false otherwise
     */
    public boolean flushPageIfDirty(int pageNum) {
        if (!isPageDirty(pageNum) || pages[pageNum] == null) {
            return true; // Nothing to flush
        }
        
        return flush(pageNum, Database.PAGE_SIZE);
    }
    
    /**
     * Flushes all dirty pages to disk.
     *
     * @return True if all pages were flushed successfully, false otherwise
     */
    public boolean flushAllDirtyPages() {
        if (dirtyPages == null) {
            return true; // Nothing to flush
        }
        
        boolean success = true;
        for (int i = dirtyPages.nextSetBit(0); i >= 0; i = dirtyPages.nextSetBit(i + 1)) {
            if (!flush(i, Database.PAGE_SIZE)) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Writes a page to disk.
     *
     * @param pageNum The page number to write
     * @param bytesToWrite The number of bytes to write
     * @return True if the page was written successfully, false otherwise
     */
    public boolean flush(int pageNum, int bytesToWrite) {
        if (pageNum >= Database.MAX_PAGES || pages[pageNum] == null) {
            return false;
        }
        
        try {
            long offset = (long) pageNum * Database.PAGE_SIZE;
            file.seek(offset);
            file.write(pages[pageNum].getData(), 0, bytesToWrite);
            
            // Mark as clean after successful write
            dirtyPages.clear(pageNum);
            
            // Update file length if necessary
            updateFileLength();
            return true;
        } catch (IOException e) {
            System.err.println("Error flushing page " + pageNum + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reads a page from disk into memory.
     *
     * @param pageNum The page number to read
     * @throws IOException If an I/O error occurs
     */
    public void readPageFromDisk(int pageNum) throws IOException {
        if (pageNum >= Database.MAX_PAGES) {
            throw new IllegalArgumentException("Page number out of bounds: " + pageNum);
        }
        
        // Calculate offset
        long offset = (long) pageNum * Database.PAGE_SIZE;
        
        // Check if page exists in file
        if (offset >= getFileLength()) {
            // Create an empty page
            pages[pageNum] = new Page(new byte[Database.PAGE_SIZE]);
            return;
        }
        
        // Read from disk
        byte[] pageData = new byte[Database.PAGE_SIZE];
        file.seek(offset);
        
        int bytesRead = file.read(pageData);
        
        if (bytesRead > 0) {
            // If we read partial page, zero out the rest
            if (bytesRead < Database.PAGE_SIZE) {
                Arrays.fill(pageData, bytesRead, Database.PAGE_SIZE, (byte) 0);
            }
            
            pages[pageNum] = new Page(pageData);
        } else {
            // Create an empty page
            pages[pageNum] = new Page(new byte[Database.PAGE_SIZE]);
        }
        
        // New page is clean
        dirtyPages.clear(pageNum);
    }
    
    /**
     * Closes the pager and releases resources.
     * 
     * @return True if closed successfully, false otherwise
     */
    public boolean close() {
        try {
            // Flush any dirty pages if they exist
            if (dirtyPages != null) {
                flushAllDirtyPages();
            }
            
            // Close the file
            if (file != null) {
                file.close();
                file = null;
            }
            
            // Clean up references but in a safe way
            if (pages != null) {
                for (int i = 0; i < pages.length; i++) {
                    pages[i] = null;
                }
                pages = null;
            }
            
            // Clear dirty pages tracking
            if (dirtyPages != null) {
                dirtyPages.clear();
                dirtyPages = null;
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error closing pager: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error during pager close: " + e.getMessage());
            return false;
        }
    }
}