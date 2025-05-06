public class Table {
    private int numRows;
    private Pager pager;

    public Table(Pager pager) {
        this.pager = pager;
        this.numRows = pager.getFileLength() / Database.ROW_SIZE;
    }

    public int getNumRows() {
        return numRows;
    }

    public Pager getPager() {
        return pager;
    }

    public ExecuteResult executeInsert(Statement statement) {
        if (numRows >= Database.TABLE_MAX_ROWS) {
            return ExecuteResult.EXECUTE_TABLE_FULL;
        }

        Row row = statement.getRowToInsert();
        Cursor cursor = tableEnd();
        int currentPage, currentRow;
        int[] cursorValues = cursorValue(cursor);
        currentPage = cursorValues[0];
        currentRow = cursorValues[1];

        serializeRow(row, currentPage, currentRow);
        return ExecuteResult.EXECUTE_SUCCESS;
    }

    public ExecuteResult executeSelect(Statement statement) {
        Cursor cursor = tableStart();

        while (!cursor.isEndOfTable()) {
            Row row = deserializeRow(cursor);
            printRow(row);
            cursor.advance();
        }
        return ExecuteResult.EXECUTE_SUCCESS;
    }

    public Cursor tableStart() {
        return new Cursor(this);
    }

    public Cursor tableEnd() {
        return new Cursor(this);
    }

    public int[] cursorValue(Cursor cursor) {
        int rowNum = cursor.getRowNum();
        int pageNum = rowNum / Database.ROWS_PER_PAGE;
        Page page = getPage(pageNum);

        int rowOffset = rowNum % Database.ROWS_PER_PAGE;
        return new int[] { pageNum, rowOffset };
    }

    public Page getPage(int pageNum) {
        Database.readPage(pager, pageNum);
        return pager.getPages()[pageNum];
    }

    public void serializeRow(Row row, int currentPage, int currentRow) {
        Page page = getPage(currentPage);
        byte[] data = new byte[Database.ROW_SIZE];

        // Serialize row into byte array
        data[0] = (byte) row.getId();
        System.arraycopy(row.getUsername().getBytes(), 0, data, 4, row.getUsername().length());
        System.arraycopy(row.getEmail().getBytes(), 0, data, 36, row.getEmail().length());

        // Write data into page
        System.arraycopy(data, 0, page.getData(), currentRow * Database.ROW_SIZE, data.length);

        numRows++;
    }

    public Row deserializeRow(Cursor cursor) {
        int[] cursorValues = cursorValue(cursor);
        int pageNum = cursorValues[0];
        int rowOffset = cursorValues[1];

        Page page = getPage(pageNum);
        byte[] rowData = new byte[Database.ROW_SIZE];
        System.arraycopy(page.getData(), rowOffset * Database.ROW_SIZE, rowData, 0, Database.ROW_SIZE);

        int id = rowData[0];  // Example: Row id is byte[0], adjust as needed.
        String username = new String(rowData, 4, 32).trim();
        String email = new String(rowData, 36, 255).trim();

        return new Row(id, username, email);
    }

    public void printRow(Row row) {
        System.out.printf("(%d, %s, %s)\n", row.getId(), row.getUsername(), row.getEmail());
    }
}
