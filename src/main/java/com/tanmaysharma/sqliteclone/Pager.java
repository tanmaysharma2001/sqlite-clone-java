package com.tanmaysharma.sqliteclone;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Pager {
    private RandomAccessFile file;
    private Page[] pages;
    private int fileLength;
    private String filename;

    public Pager(String filename) {
        this.filename = filename;
        try {
            File dbFile = new File(filename);
            
            // Check if the file exists
            boolean fileExists = dbFile.exists() && dbFile.length() > 0;
            
            if (!fileExists) {
                System.out.println("File does not exist or is empty. Creating new file.");
            }
            
            // Open the file for read/write
            this.file = new RandomAccessFile(filename, "rw");
            this.fileLength = (int) file.length();
            this.pages = new Page[Database.MAX_PAGES];
            
            // Initialize all pages to null
            for (int i = 0; i < Database.MAX_PAGES; i++) {
                pages[i] = null;
            }
            
        } catch (IOException e) {
            System.err.println("Error opening file: " + filename);
            System.exit(1);
        }
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public Page[] getPages() {
        return pages;
    }

    public int getFileLength() {
        return fileLength;
    }
    
    public void updateFileLength() {
        try {
            this.fileLength = (int) file.length();
        } catch (IOException e) {
            System.err.println("Error getting file length: " + e.getMessage());
        }
    }

    public void flush(int pageNum, int bytesToWrite) {
        if (pageNum >= Database.MAX_PAGES) {
            System.err.println("Attempted to flush invalid page number: " + pageNum);
            return;
        }
        
        if (pages[pageNum] == null) {
            System.err.println("Attempted to flush null page: " + pageNum);
            return;
        }
        
        try {
            long offset = (long) pageNum * Database.PAGE_SIZE;
            file.seek(offset);
            file.write(pages[pageNum].getData(), 0, bytesToWrite);
            file.getFD().sync(); // Ensure data is written to disk
            updateFileLength();
        } catch (IOException e) {
            System.err.println("Error flushing page " + pageNum + ": " + e.getMessage());
        }
    }
    
    public void readPageFromDisk(int pageNum) throws IOException {
        if (pageNum >= Database.MAX_PAGES) {
            throw new IllegalArgumentException("Page number out of bounds: " + pageNum);
        }
        
        // Don't try to read beyond the file
        if ((long)pageNum * Database.PAGE_SIZE >= file.length()) {
            // Initialize an empty page
            pages[pageNum] = new Page(new byte[Database.PAGE_SIZE]);
            return;
        }
        
        byte[] pageData = new byte[Database.PAGE_SIZE];
        long offset = (long) pageNum * Database.PAGE_SIZE;
        
        file.seek(offset);
        
        // Read as much as available
        int bytesRead = file.read(pageData);
        
        // If we read something
        if (bytesRead > 0) {
            pages[pageNum] = new Page(pageData);
        } else {
            // Empty page
            pages[pageNum] = new Page(new byte[Database.PAGE_SIZE]);
        }
    }
}