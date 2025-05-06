package com.tanmaysharma.sqliteclone;

public class Cursor {
    public Table table;
    public int rowNum;
    public boolean endOfTable;

    // Constructor
    public Cursor(Table table, int rowNum, boolean endOfTable) {
        this.table = table;
        this.rowNum = rowNum;
        this.endOfTable = endOfTable;
    }

    // Start of the table
    public static Cursor tableStart(Table table) {
        Cursor cursor = new Cursor(table, 0, table.numRows == 0);
        return cursor;
    }

    // End of the table
    public static Cursor tableEnd(Table table) {
        Cursor cursor = new Cursor(table, table.numRows, true);
        return cursor;
    }

    // Move to the next row
    public void advance() {
        rowNum += 1;
        if (rowNum == table.numRows) {
            endOfTable = true;
        }
    }
}
