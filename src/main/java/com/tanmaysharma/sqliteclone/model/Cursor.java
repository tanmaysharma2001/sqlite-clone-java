package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.exception.ValidationException;

/**
 * Represents a cursor for traversing table rows.
 * Provides methods for navigating through rows in a table with bounds checking.
 */
public class Cursor {
    private final int maxRows;
    private int currentRow;
    private boolean endOfTable;

    /**
     * Creates a new cursor with the specified maximum row count.
     *
     * @param maxRows The maximum number of rows in the table
     * @throws ValidationException if maxRows is invalid
     */
    public Cursor(int maxRows) {
        if (maxRows < 0) {
            throw new ValidationException("Max rows cannot be negative", "maxRows", maxRows);
        }

        this.maxRows = maxRows;
        this.currentRow = 0;
        this.endOfTable = (maxRows == 0);
    }

    /**
     * Creates a cursor starting at a specific row.
     *
     * @param maxRows The maximum number of rows
     * @param startRow The starting row position
     * @throws ValidationException if parameters are invalid
     */
    public Cursor(int maxRows, int startRow) {
        if (maxRows < 0) {
            throw new ValidationException("Max rows cannot be negative", "maxRows", maxRows);
        }

        if (startRow < 0) {
            throw new ValidationException("Start row cannot be negative", "startRow", startRow);
        }

        this.maxRows = maxRows;
        this.currentRow = startRow;
        this.endOfTable = (startRow >= maxRows);
    }

    /**
     * Gets the current row number.
     *
     * @return The current row number
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * Gets the maximum number of rows.
     *
     * @return The maximum row count
     */
    public int getMaxRows() {
        return maxRows;
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
     *
     * @return True if advanced successfully, false if already at end
     */
    public boolean advance() {
        if (endOfTable) {
            return false;
        }

        currentRow++;
        if (currentRow >= maxRows) {
            endOfTable = true;
        }

        return !endOfTable;
    }

    /**
     * Moves the cursor to the previous row.
     *
     * @return True if moved successfully, false if already at beginning
     */
    public boolean previous() {
        if (currentRow <= 0) {
            return false;
        }

        currentRow--;
        endOfTable = false;
        return true;
    }

    /**
     * Resets the cursor to the beginning of the table.
     */
    public void reset() {
        this.currentRow = 0;
        this.endOfTable = (maxRows == 0);
    }

    /**
     * Moves the cursor to the end of the table.
     */
    public void moveToEnd() {
        this.currentRow = maxRows;
        this.endOfTable = true;
    }

    /**
     * Seeks to a specific row number.
     *
     * @param rowNum The row number to seek to
     * @return True if seek was successful, false if out of bounds
     */
    public boolean seek(int rowNum) {
        if (rowNum < 0 || rowNum >= maxRows) {
            return false;
        }

        this.currentRow = rowNum;
        this.endOfTable = false;
        return true;
    }

    /**
     * Checks if the cursor is at the beginning of the table.
     *
     * @return True if at the beginning
     */
    public boolean isAtBeginning() {
        return currentRow == 0;
    }

    /**
     * Gets the number of rows remaining from the current position.
     *
     * @return The number of remaining rows
     */
    public int getRemainingRows() {
        if (endOfTable) {
            return 0;
        }
        return maxRows - currentRow;
    }

    /**
     * Creates a copy of this cursor.
     *
     * @return A new cursor at the same position
     */
    public Cursor copy() {
        Cursor copy = new Cursor(maxRows, currentRow);
        copy.endOfTable = this.endOfTable;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("Cursor{currentRow=%d, maxRows=%d, endOfTable=%s}",
                currentRow, maxRows, endOfTable);
    }
}