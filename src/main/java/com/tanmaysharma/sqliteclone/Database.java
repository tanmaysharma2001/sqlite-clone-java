package com.tanmaysharma.sqliteclone;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Database {
    public static final int ROW_SIZE = 512;  // 512 bytes per row (adjust as needed)
    public static final int TABLE_MAX_ROWS = 1000;
    public static final int PAGE_SIZE = 4096;  // 4 KB page size, same as the original
    public static final int MAX_PAGES = 100;
    public static final int ROWS_PER_PAGE = 100;  // Adjust according to your need


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
                pager.getPages()[i] = null;
            }
        }

        // Write the remaining page with partial rows
        int numAdditionalRows = table.getNumRows() % ROWS_PER_PAGE;
        if (numAdditionalRows > 0) {
            int pageNum = numFullPages;
            if (pager.getPages()[pageNum] != null) {
                pager.flush(pageNum, numAdditionalRows * ROW_SIZE);
                pager.getPages()[pageNum] = null;
            }
        }

        try {
            pager.getFile().close();
        } catch (IOException e) {
            System.err.println("Error closing file: " + e.getMessage());
        }
    }

    public static void readPage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            System.err.println("Tried to read page number out of bounds: " + pageNum);
            System.exit(1);
        }

        if (pager.getPages()[pageNum] == null) {
            // Page cache miss, load from disk
            try {
                byte[] pageData = new byte[PAGE_SIZE];
                long offset = (long) pageNum * PAGE_SIZE;
                RandomAccessFile file = pager.getFile();
                file.seek(offset);
                int bytesRead = file.read(pageData);
                if (bytesRead < PAGE_SIZE) {
                    System.err.println("Error reading page from file.");
                    System.exit(1);
                }

                Page page = new Page(pageData);
                pager.getPages()[pageNum] = page;
            } catch (IOException e) {
                System.err.println("Error reading page: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    public static void writePage(Pager pager, int pageNum) {
        if (pageNum >= MAX_PAGES) {
            System.err.println("Tried to write page number out of bounds: " + pageNum);
            System.exit(1);
        }

        Page page = pager.getPages()[pageNum];
        if (page != null) {
            try {
                long offset = (long) pageNum * PAGE_SIZE;
                RandomAccessFile file = pager.getFile();
                file.seek(offset);
                file.write(page.getData());
            } catch (IOException e) {
                System.err.println("Error writing page to file: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
