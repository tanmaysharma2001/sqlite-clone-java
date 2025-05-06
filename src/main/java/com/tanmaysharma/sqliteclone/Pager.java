package com.tanmaysharma.sqliteclone;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Pager {
    private RandomAccessFile file;
    private Page[] pages;
    private int fileLength;

    public Pager(String filename) {
        try {
            this.file = new RandomAccessFile(filename, "rw");
            this.fileLength = (int) file.length();
            this.pages = new Page[Database.MAX_PAGES];
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

    public void flush(int pageNum, int bytesToWrite) {
        if (pages[pageNum] != null) {
            try {
                long offset = (long) pageNum * Database.PAGE_SIZE;
                file.seek(offset);
                file.write(pages[pageNum].getData(), 0, bytesToWrite);
            } catch (IOException e) {
                System.err.println("Error flushing page: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
