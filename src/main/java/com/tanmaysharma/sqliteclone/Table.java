package com.tanmaysharma.sqliteclone;

public class Table {
    private int numRows;
    private Pager pager;
    private int validRows; // Track actual valid rows

    public Table(Pager pager) {
        this.pager = pager;
        this.validRows = 0;
        
        // Calculate raw row count based on file size
        if (pager.getFileLength() > 0) {
            // Calculate number of rows that would fit in the file
            int potentialRows = pager.getFileLength() / Database.ROW_SIZE;
            this.numRows = Math.min(potentialRows, Database.TABLE_MAX_ROWS);
            
            // Count valid rows (with non-zero IDs)
            for (int i = 0; i < numRows; i++) {
                int pageNum = i / Database.ROWS_PER_PAGE;
                int rowOffset = i % Database.ROWS_PER_PAGE;
                
                try {
                    Row row = deserializeRowAt(pageNum, rowOffset);
                    if (row != null && row.getId() > 0) {
                        validRows++;
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                }
            }
            
            System.out.println("Database opened with " + validRows + " valid rows out of " + numRows + " total rows.");
        } else {
            this.numRows = 0;
            System.out.println("Empty database opened. Ready for data.");
        }
    }

    public int getNumRows() {
        return numRows;
    }
    
    public int getValidRows() {
        return validRows;
    }

    public Pager getPager() {
        return pager;
    }

    public ExecuteResult executeInsert(Statement statement) {
        if (numRows >= Database.TABLE_MAX_ROWS) {
            return ExecuteResult.EXECUTE_TABLE_FULL;
        }

        Row row = statement.getRowToInsert();
        
        // Calculate the page and row offset for insertion
        int pageNum = numRows / Database.ROWS_PER_PAGE;
        int rowOffset = numRows % Database.ROWS_PER_PAGE;
        
        // Make sure we're not exceeding page capacity
        if (rowOffset * Database.ROW_SIZE >= Database.PAGE_SIZE) {
            System.err.println("Error: Row won't fit in page");
            return ExecuteResult.EXECUTE_FAIL;
        }

        // Serialize the row to the appropriate page and offset
        if (serializeRow(row, pageNum, rowOffset)) {
            // Increment the row count only if serialization succeeded
            numRows++;
            validRows++;
            return ExecuteResult.EXECUTE_SUCCESS;
        } else {
            return ExecuteResult.EXECUTE_FAIL;
        }
    }

    public ExecuteResult executeSelect(Statement statement) {
        int found = 0;
        
        // Only process up to our known row count
        for (int i = 0; i < numRows; i++) {
            int pageNum = i / Database.ROWS_PER_PAGE;
            int rowOffset = i % Database.ROWS_PER_PAGE;
            
            // Skip if row position would be out of bounds
            if (rowOffset * Database.ROW_SIZE >= Database.PAGE_SIZE) {
                continue;
            }
            
            try {
                Row row = deserializeRowAt(pageNum, rowOffset);
                // Only print rows with valid data (non-zero ID)
                if (row != null && row.getId() > 0) {
                    printRow(row);
                    found++;
                }
            } catch (Exception e) {
                // Just skip problematic rows
            }
        }
        
        if (found == 0) {
            System.out.println("No rows found.");
        } else {
            System.out.println("Found " + found + " row(s).");
        }
        
        return ExecuteResult.EXECUTE_SUCCESS;
    }

    public Page getPage(int pageNum) {
        if (pageNum >= Database.MAX_PAGES) {
            System.err.println("Tried to access page number out of bounds: " + pageNum);
            return null;
        }
        
        if (pager.getPages()[pageNum] == null) {
            // Initialize an empty page if it doesn't exist
            pager.getPages()[pageNum] = new Page(new byte[Database.PAGE_SIZE]);
            
            // Try to read existing data if file contains this page
            if (pageNum * Database.PAGE_SIZE < pager.getFileLength()) {
                try {
                    pager.readPageFromDisk(pageNum);
                } catch (Exception e) {
                    System.err.println("Warning: Could not read page " + pageNum + " from disk.");
                }
            }
        }
        
        return pager.getPages()[pageNum];
    }

    public boolean serializeRow(Row row, int pageNum, int rowOffset) {
        Page page = getPage(pageNum);
        if (page == null) {
            System.err.println("Failed to get page " + pageNum);
            return false;
        }
        
        byte[] data = new byte[Database.ROW_SIZE];
        
        // Convert int to bytes (4 bytes for id)
        data[0] = (byte) (row.getId() & 0xFF);
        data[1] = (byte) ((row.getId() >> 8) & 0xFF);
        data[2] = (byte) ((row.getId() >> 16) & 0xFF);
        data[3] = (byte) ((row.getId() >> 24) & 0xFF);
        
        // Copy username (with bounds checking)
        byte[] usernameBytes = row.getUsername().getBytes();
        int usernameCopyLength = Math.min(usernameBytes.length, 32);
        System.arraycopy(usernameBytes, 0, data, 4, usernameCopyLength);
        
        // Copy email (with bounds checking)
        byte[] emailBytes = row.getEmail().getBytes();
        int emailCopyLength = Math.min(emailBytes.length, 255);
        System.arraycopy(emailBytes, 0, data, 36, emailCopyLength);
        
        // Calculate offset in the page data array
        int offset = rowOffset * Database.ROW_SIZE;
        
        // Ensure we don't exceed page size
        if (offset + Database.ROW_SIZE <= Database.PAGE_SIZE) {
            System.arraycopy(data, 0, page.getData(), offset, Database.ROW_SIZE);
            
            // Make sure to flush the page to disk immediately
            pager.flush(pageNum, Database.PAGE_SIZE);
            
            return true;
        } else {
            System.err.println("Error: Row offset exceeds page size");
            return false;
        }
    }
    
    public Row deserializeRowAt(int pageNum, int rowOffset) {
        // Ensure page is loaded
        Page page = getPage(pageNum);
        if (page == null) {
            return null;
        }
        
        // Calculate offset in the page data array
        int offset = rowOffset * Database.ROW_SIZE;
        
        // Check if we're trying to read beyond page bounds
        if (offset + Database.ROW_SIZE > Database.PAGE_SIZE) {
            return null;
        }
        
        byte[] rowData = new byte[Database.ROW_SIZE];
        System.arraycopy(page.getData(), offset, rowData, 0, Database.ROW_SIZE);
        
        // Reconstruct ID from 4 bytes
        int id = (rowData[0] & 0xFF) |
                ((rowData[1] & 0xFF) << 8) |
                ((rowData[2] & 0xFF) << 16) |
                ((rowData[3] & 0xFF) << 24);
        
        // Don't bother with the rest if ID is 0 (empty row)
        if (id == 0) {
            return new Row(0, "", "");
        }
        
        // Extract strings with proper handling of null bytes
        String username = extractString(rowData, 4, 32);
        String email = extractString(rowData, 36, 255);
        
        return new Row(id, username, email);
    }
    
    private String extractString(byte[] data, int offset, int maxLength) {
        // Find the end of string (null byte or end of allocated space)
        int end = offset;
        int maxEnd = Math.min(offset + maxLength, data.length);
        
        while (end < maxEnd && data[end] != 0) {
            end++;
        }
        
        // Extract the string
        if (end > offset) {
            return new String(data, offset, end - offset).trim();
        } else {
            return "";
        }
    }

    public void printRow(Row row) {
        if (row != null) {
            System.out.printf("(%d, %s, %s)%n", row.getId(), row.getUsername(), row.getEmail());
        }
    }
}