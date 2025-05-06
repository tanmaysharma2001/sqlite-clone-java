package com.tanmaysharma.sqliteclone;

/**
 * Represents a cursor for traversing table rows.
 * Provides methods for navigating through rows in a table.
 */
public class Cursor {
    private final Table table;
    private int rowNum;
    private boolean endOfTable;

    /**
     * Creates a new cursor for the specified table.
     *
     * @param table The table to traverse
     * @throws IllegalArgumentException if the table is null
     */
    public Cursor(Table table) {
        if (table == null) {
            throw new IllegalArgumentException("Table cannot be null");
        }
        
        this.table = table;
        this.rowNum = 0;
        this.endOfTable = (table.getNumRows() == 0);
    }

    /**
     * Checks if the cursor has reached the end of the table.
     *
     * @return True if at the end of the table, false otherwise
     */
    public boolean isEndOfTable() {
        return endOfTable;
    }

    /**
     * Advances the cursor to the next row.
     */
    public void advance() {
        rowNum++;
        if (rowNum >= table.getNumRows()) {
            endOfTable = true;
        }
    }

    /**
     * Gets the current row number.
     *
     * @return The current row number
     */
    public int getRowNum() {
        return this.rowNum;
    }
    
    /**
     * Sets the row number.
     *
     * @param rowNum The row number to set
     * @throws IllegalArgumentException if the row number is negative
     */
    public void setRowNum(int rowNum) {
        if (rowNum < 0) {
            throw new IllegalArgumentException("Row number cannot be negative");
        }
        
        this.rowNum = rowNum;
        this.endOfTable = (rowNum >= table.getNumRows());
    }
    
    /**
     * Gets the current row data.
     *
     * @return The current row, or null if at the end of the table
     */
    public Row getCurrentRow() {
        if (isEndOfTable()) {
            return null;
        }
        
        int pageNum = rowNum / Database.ROWS_PER_PAGE;
        int rowOffset = rowNum % Database.ROWS_PER_PAGE;
        
        return table.deserializeRowAt(pageNum, rowOffset);
    }
    
    /**
     * Resets the cursor to the beginning of the table.
     */
    public void reset() {
        this.rowNum = 0;
        this.endOfTable = (table.getNumRows() == 0);
    }
    
    /**
     * Seeks to a specific row number.
     *
     * @param rowNum The row number to seek to
     * @return True if seek was successful, false if out of bounds
     */
    public boolean seek(int rowNum) {
        if (rowNum < 0 || rowNum >= table.getNumRows()) {
            return false;
        }
        
        this.rowNum = rowNum;
        this.endOfTable = false;
        return true;
    }
}