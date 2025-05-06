package com.tanmaysharma.sqliteclone;

import java.io.IOException;

public class Database {
    public static final int ROW_SIZE = 292;  // 4 bytes for ID + 32 bytes for username + 256 bytes for email
    public static final int TABLE_MAX_ROWS = 1000;
    public static final int PAGE_SIZE = 4096;  // 4 KB page size
    public static final int MAX_PAGES = 100;
    public static final int ROWS_PER_PAGE = PAGE_SIZE / ROW_SIZE;  // Number of rows that fit in a page

    public static Table open(String filename) {
        Pager pager = new Pager(filename);
        return new Table(pager);
    }

    public static void close(Table table) {
        Pager pager = table.getPager();
        int numFullPages = table.getNumRows() / ROWS_PER_PAGE;

        // Write any full pages
        for (int i = 0; i < numFullPages; i++) {
            if (pager.getPages()[i] != null) {
                pager.flush(i, PAGE_SIZE);
            }
        }

        // Write the remaining page with partial rows
        int numAdditionalRows = table.getNumRows() % ROWS_PER_PAGE;
        if (numAdditionalRows > 0) {
            int pageNum = numFullPages;
            if (pager.getPages()[pageNum] != null) {
                pager.flush(pageNum, PAGE_SIZE);
            }
        }

        try {
            pager.getFile().close();
            System.out.println("Database closed successfully.");
        } catch (IOException e) {
            System.err.println("Error closing file: " + e.getMessage());
        }
    }

    public static void readPage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            System.err.println("Tried to read page number out of bounds: " + pageNum);
            return;
        }

        if (pager.getPages()[pageNum] == null) {
            // Page cache miss, load from disk
            try {
                pager.readPageFromDisk(pageNum);
            } catch (IOException e) {
                System.err.println("Error reading page " + pageNum + ": " + e.getMessage());
                
                // Initialize empty page if read fails
                pager.getPages()[pageNum] = new Page(new byte[PAGE_SIZE]);
            }
        }
    }

    public static void writePage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            System.err.println("Tried to write page number out of bounds: " + pageNum);
            return;
        }

        if (pager.getPages()[pageNum] != null) {
            pager.flush(pageNum, PAGE_SIZE);
        }
    }
}