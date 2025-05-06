package com.tanmaysharma.sqliteclone;

public class Cursor {
    private Table table;
    private int rowNum;
    private boolean endOfTable;

    public Cursor(Table table) {
        this.table = table;
        this.rowNum = 0;
        this.endOfTable = (table.getNumRows() == 0);
    }

    public boolean isEndOfTable() {
        return endOfTable;
    }

    public void advance() {
        rowNum++;
        if (rowNum >= table.getNumRows()) {
            endOfTable = true;
        }
    }

    public int getRowNum() {
        return this.rowNum;
    }
    
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }
    
    public void setEndOfTable(boolean endOfTable) {
        this.endOfTable = endOfTable;
    }
}