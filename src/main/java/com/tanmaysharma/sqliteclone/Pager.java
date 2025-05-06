package com.tanmaysharma.sqliteclone;

import java.io.RandomAccessFile;
import java.io.IOException;

public class Pager {
    public RandomAccessFile file;
    public int fileDescriptor;
    public int numPages;
    public int fileLength;
    public Page[] pages = new Page[Table.TABLE_MAX_PAGES];

    public Pager(String filename) throws IOException {
        // Open the file for read/write access, or create it if it doesn't exist
        file = new RandomAccessFile(filename, "rw");
        fileDescriptor = (int) file.getFD();
        fileLength = (int) file.length();
        numPages = fileLength / Table.PAGE_SIZE;

        // Initialize pages array
        for (int i = 0; i < Table.TABLE_MAX_PAGES; i++) {
            pages[i] = null;
        }
    }

    // Get the page for a given page number
    public Page getPage(int pageNum) throws IOException {
        if (pageNum >= Table.TABLE_MAX_PAGES) {
            throw new IllegalStateException("Tried to fetch page number out of bounds: " + pageNum);
        }

        if (pages[pageNum] == null) {
            // Cache miss, create a new page and load it from the file
            Page page = new Page();
            long offset = (long) pageNum * Table.PAGE_SIZE;
            file.seek(offset);
            file.read(page.rows);
            pages[pageNum] = page;
        }

        return pages[pageNum];
    }

    // Write the page to disk
    public void flushPage(int pageNum) throws IOException {
        if (pages[pageNum] == null) {
            throw new IllegalStateException("Tried to flush null page.");
        }

        Page page = pages[pageNum];
        long offset = (long) pageNum * Table.PAGE_SIZE;
        file.seek(offset);
        file.write(page.rows);
    }

    // Close the file
    public void close() throws IOException {
        file.close();
    }
}
