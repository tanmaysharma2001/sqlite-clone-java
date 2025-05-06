package com.tanmaysharma.sqliteclone;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Table {
    // Row definition constants
    public static final int COLUMN_ID_SIZE = 4;
    public static final int COLUMN_USERNAME_SIZE = 32;
    public static final int COLUMN_EMAIL_SIZE = 255;

    public static final int ROW_SIZE = COLUMN_ID_SIZE + COLUMN_USERNAME_SIZE + COLUMN_EMAIL_SIZE + 2;

    // Page size is 4KB, similar to memory pages
    public static final int PAGE_SIZE = 4096;
    public static final int TABLE_MAX_PAGES = 100;
    public static final int ROWS_PER_PAGE = PAGE_SIZE / ROW_SIZE;
    public static final int TABLE_MAX_ROWS = ROWS_PER_PAGE * TABLE_MAX_PAGES;

    // Row class definition
    public static class Row {
        public int id;
        public String username;
        public String email;

        public Row(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }
    }

    // Page class definition
    public static class Page {
        public byte[] rows;
        public int numberOfRows;
        public long num;

        public Page() {
            rows = new byte[PAGE_SIZE];
        }
    }

    // Pager class for handling file operations
    public static class Pager {
        public RandomAccessFile file;
        public int fileDescriptor;
        public int fileLength;
        public Page[] pages = new Page[TABLE_MAX_PAGES];

        public Pager(String filename) throws IOException {
            file = new RandomAccessFile(filename, "rw");
            fileDescriptor = (int) file.getFD();
            fileLength = (int) file.length();
        }

        public Page getPage(int pageNum) throws IOException {
            if (pageNum >= TABLE_MAX_PAGES) {
                throw new IllegalStateException("Tried to fetch page number out of bounds: " + pageNum);
            }

            if (pages[pageNum] == null) {
                // Cache miss, create a new page and load it
                Page page = new Page();
                long offset = pageNum * PAGE_SIZE;
                file.seek(offset);
                file.read(page.rows);
                pages[pageNum] = page;
            }

            return pages[pageNum];
        }

        public void flushPage(int pageNum) throws IOException {
            if (pages[pageNum] == null) {
                throw new IllegalStateException("Tried to flush null page.");
            }

            Page page = pages[pageNum];
            long offset = pageNum * PAGE_SIZE;
            file.seek(offset);
            file.write(page.rows);
        }
    }

    // Table class with rows and pager
    private int numRows;
    private Pager pager;

    public Table(Pager pager) {
        this.pager = pager;
        this.numRows = pager.fileLength / ROW_SIZE;
    }

    public void close() throws IOException {
        int fullPages = numRows / ROWS_PER_PAGE;
        for (int i = 0; i < fullPages; i++) {
            if (pager.pages[i] != null) {
                pager.flushPage(i);
                pager.pages[i] = null;
            }
        }

        int remainingRows = numRows % ROWS_PER_PAGE;
        if (remainingRows > 0) {
            int pageNum = fullPages;
            if (pager.pages[pageNum] != null) {
                pager.flushPage(pageNum);
                pager.pages[pageNum] = null;
            }
        }

        pager.file.close();
    }

    // Serialize a row into byte[] and save it to the page
    public void serializeRow(Row row, int currentPage, int currentRow) throws IOException {
        Page page = pager.pages[currentPage];
        int rowIndex = currentRow * ROW_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(ROW_SIZE);

        buffer.putInt(row.id);
        byte[] usernameBytes = padString(row.username, COLUMN_USERNAME_SIZE).getBytes(StandardCharsets.UTF_8);
        buffer.put(usernameBytes);
        byte[] emailBytes = padString(row.email, COLUMN_EMAIL_SIZE).getBytes(StandardCharsets.UTF_8);
        buffer.put(emailBytes);

        System.arraycopy(buffer.array(), 0, page.rows, rowIndex, ROW_SIZE);
        numRows += 1;
    }

    // Deserialize a row from byte[] and return the Row object
    public Row deserializeRow(int currentPage, int currentRow) {
        Page page = pager.pages[currentPage];
        int rowIndex = currentRow * ROW_SIZE;
        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(page.rows, rowIndex, rowIndex + ROW_SIZE));

        int id = buffer.getInt();
        byte[] usernameBytes = new byte[COLUMN_USERNAME_SIZE];
        buffer.get(usernameBytes);
        String username = new String(usernameBytes, StandardCharsets.UTF_8).trim();

        byte[] emailBytes = new byte[COLUMN_EMAIL_SIZE];
        buffer.get(emailBytes);
        String email = new String(emailBytes, StandardCharsets.UTF_8).trim();

        return new Row(id, username, email);
    }

    // Helper method to pad strings
    private String padString(String str, int length) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }

    // Get the current page for a given row
    public int getPageForRow(int rowNum) {
        return rowNum / ROWS_PER_PAGE;
    }

    // Fetch a specific row from the table
    public Row getRow(int rowNum) throws IOException {
        int pageNum = getPageForRow(rowNum);
        Page page = pager.getPage(pageNum);
        int rowOffset = rowNum % ROWS_PER_PAGE;
        return deserializeRow(pageNum, rowOffset);
    }
}
