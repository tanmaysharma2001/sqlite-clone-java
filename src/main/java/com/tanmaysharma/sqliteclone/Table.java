package com.tanmaysharma.sqliteclone;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a database table with methods for row insertion and selection.
 * Thread-safe implementation with proper resource management and indexing.
 */
public class Table {
    private volatile int numRows;
    private final Pager pager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int ID_SIZE = 4;
    private static final int USERNAME_SIZE = 32;
    private static final int EMAIL_SIZE = 256;
    private static final int ID_OFFSET = 0;
    private static final int USERNAME_OFFSET = ID_OFFSET + ID_SIZE;
    private static final int EMAIL_OFFSET = USERNAME_OFFSET + USERNAME_SIZE;
    
    // Add index for faster lookups
    private final Map<Integer, Integer> idToRowNum = new HashMap<>();
    
    // Add logging
    private static final Logger logger = Logger.getLogger(Table.class.getName());

    /**
     * Creates a table with the provided pager.
     *
     * @param pager The pager to use for storage operations
     */
    public Table(Pager pager) {
        if (pager == null) {
            throw new IllegalArgumentException("Pager cannot be null");
        }
        
        this.pager = pager;
        this.numRows = calculateRowCount();
        
        // Initialize index for existing rows
        initializeIndex();
        
        logger.info("Database opened with " + numRows + " rows.");
    }
    
    /**
     * Initialize the index for existing rows.
     * This scans the table and builds an ID-to-rowNum index.
     */
    private void initializeIndex() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numRows; i++) {
                int pageNum = i / Database.ROWS_PER_PAGE;
                int rowOffset = i % Database.ROWS_PER_PAGE;
                
                Row row = deserializeRowAt(pageNum, rowOffset);
                if (row != null) {
                    idToRowNum.put(row.getId(), i);
                }
            }
            logger.info("Initialized index with " + idToRowNum.size() + " entries");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Calculates the number of rows based on file size.
     *
     * @return The number of rows in the table
     */
    private int calculateRowCount() {
        if (pager.getFileLength() <= 0) {
            return 0;
        }
        
        // Calculate potential rows based on file length
        int potentialRows = pager.getFileLength() / Database.ROW_SIZE;
        return Math.min(potentialRows, Database.TABLE_MAX_ROWS);
    }

    /**
     * Gets the number of rows in the table.
     *
     * @return The number of rows
     */
    public int getNumRows() {
        return numRows;
    }

    /**
     * Gets the pager for this table.
     *
     * @return The pager
     */
    public Pager getPager() {
        return pager;
    }

    /**
     * Executes an INSERT statement.
     *
     * @param statement The statement containing the row to insert
     * @return The result of the execution
     */
    public ExecuteResult executeInsert(Statement statement) {
        lock.writeLock().lock();
        try {
            if (numRows >= Database.TABLE_MAX_ROWS) {
                return ExecuteResult.EXECUTE_TABLE_FULL;
            }

            Row row = statement.getRowToInsert();
            if (row == null) {
                return ExecuteResult.EXECUTE_FAIL;
            }
            
            // Check for duplicate ID
            if (idToRowNum.containsKey(row.getId())) {
                logger.warning("Duplicate ID on insert: " + row.getId());
                return ExecuteResult.EXECUTE_FAIL;
            }
            
            // Validate row data
            if (!validateRowData(row)) {
                return ExecuteResult.EXECUTE_FAIL;
            }

            // Calculate page and row offset
            int pageNum = numRows / Database.ROWS_PER_PAGE;
            int rowOffset = numRows % Database.ROWS_PER_PAGE;

            // Serialize the row
            if (serializeRow(row, pageNum, rowOffset)) {
                // Add to index and increment row count
                idToRowNum.put(row.getId(), numRows);
                numRows++;
                return ExecuteResult.EXECUTE_SUCCESS;
            } else {
                return ExecuteResult.EXECUTE_FAIL;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Validates row data before insertion.
     *
     * @param row The row to validate
     * @return True if the row is valid, false otherwise
     */
    private boolean validateRowData(Row row) {
        // Check ID
        if (row.getId() <= 0) {
            logger.warning("Invalid ID (must be positive): " + row.getId());
            return false;
        }
        
        // Check username
        if (row.getUsername() == null || row.getUsername().isEmpty()) {
            logger.warning("Username cannot be empty");
            return false;
        }
        
        if (row.getUsername().length() > USERNAME_SIZE) {
            logger.warning("Username will be truncated (max " + USERNAME_SIZE + " bytes): " + 
                          row.getUsername());
        }
        
        // Check email
        if (row.getEmail() == null) {
            logger.warning("Email cannot be null");
            return false;
        }
        
        if (row.getEmail().length() > EMAIL_SIZE) {
            logger.warning("Email will be truncated (max " + EMAIL_SIZE + " bytes): " + 
                          row.getEmail());
        }
        
        return true;
    }

    /**
     * Executes a SELECT statement.
     *
     * @param statement The statement to execute
     * @return The result of the execution
     */
    public ExecuteResult executeSelect(Statement statement) {
        lock.readLock().lock();
        try {
            List<Row> rows = new ArrayList<>();
            
            // Check if we have a WHERE clause with ID filter
            String whereClause = statement.getWhereClause();
            if (whereClause != null && whereClause.toLowerCase().contains("id =")) {
                // Extract ID value (very simplified parsing)
                try {
                    int id = extractIdFromWhere(whereClause);
                    // Use index for lookup
                    Integer rowNum = idToRowNum.get(id);
                    if (rowNum != null) {
                        int pageNum = rowNum / Database.ROWS_PER_PAGE;
                        int rowOffset = rowNum % Database.ROWS_PER_PAGE;
                        
                        Row row = deserializeRowAt(pageNum, rowOffset);
                        if (row != null) {
                            printRow(row);
                            return ExecuteResult.EXECUTE_SUCCESS;
                        }
                    }
                    
                    // No matching row
                    logger.info("No rows found for ID: " + id);
                    return ExecuteResult.EXECUTE_SUCCESS;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error parsing WHERE clause", e);
                    // Fall back to full table scan
                }
            }
            
            // Full table scan
            int rowsFound = 0;
            for (int i = 0; i < numRows; i++) {
                int pageNum = i / Database.ROWS_PER_PAGE;
                int rowOffset = i % Database.ROWS_PER_PAGE;
                
                Row row = deserializeRowAt(pageNum, rowOffset);
                if (row != null && row.getId() > 0) {
                    printRow(row);
                    rowsFound++;
                }
            }
            
            if (rowsFound == 0) {
                logger.info("No rows found.");
            } else {
                logger.info("Found " + rowsFound + " row(s).");
            }
            
            return ExecuteResult.EXECUTE_SUCCESS;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing SELECT", e);
            return ExecuteResult.EXECUTE_FAIL;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Extract ID value from a simple WHERE clause.
     * This is a very simplified parser that only handles "id = X" clauses.
     * 
     * @param whereClause The WHERE clause to parse
     * @return The extracted ID
     * @throws IllegalArgumentException if the clause cannot be parsed
     */
    private int extractIdFromWhere(String whereClause) {
        if (whereClause == null) {
            throw new IllegalArgumentException("WHERE clause is null");
        }
        
        // Very simplified parsing - in a real database, use a proper SQL parser
        String[] parts = whereClause.split("=");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid WHERE clause format: " + whereClause);
        }
        
        String idPart = parts[0].trim();
        String valuePart = parts[1].trim();
        
        if (!idPart.equalsIgnoreCase("id")) {
            throw new IllegalArgumentException("Only ID filters are supported: " + whereClause);
        }
        
        try {
            return Integer.parseInt(valuePart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID value: " + valuePart);
        }
    }

    /**
     * Gets or loads a page from the pager.
     *
     * @param pageNum The page number to get
     * @return The requested page or null if an error occurs
     */
    private Page getPage(int pageNum) {
        if (pageNum >= Database.MAX_PAGES) {
            logger.warning("Tried to access page number out of bounds: " + pageNum);
            return null;
        }
        
        // Check if page is already loaded
        if (pager.getPages()[pageNum] == null) {
            try {
                // Load the page from disk if it exists
                Database.readPage(pager, pageNum);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to read page " + pageNum, e);
                return null;
            }
        }
        
        return pager.getPages()[pageNum];
    }

    /**
     * Serializes a row to the specified page and row offset.
     *
     * @param row The row to serialize
     * @param pageNum The page number to write to
     * @param rowOffset The row offset within the page
     * @return True if serialization was successful, false otherwise
     */
    public boolean serializeRow(Row row, int pageNum, int rowOffset) {
        Page page = getPage(pageNum);
        if (page == null) {
            return false;
        }
        
        // Calculate offset in the page data array
        int offset = rowOffset * Database.ROW_SIZE;
        
        // Ensure offset is within page bounds
        if (offset + Database.ROW_SIZE > Database.PAGE_SIZE) {
            logger.warning("Error: Row offset exceeds page size");
            return false;
        }
        
        byte[] data = page.getData();
        
        // Write ID (4 bytes) - write in little-endian format
        int id = row.getId();
        data[offset] = (byte) (id & 0xFF);
        data[offset + 1] = (byte) ((id >> 8) & 0xFF);
        data[offset + 2] = (byte) ((id >> 16) & 0xFF);
        data[offset + 3] = (byte) ((id >> 24) & 0xFF);
        
        // Write username (fixed-width field)
        writeFixedStringDirect(data, offset + USERNAME_OFFSET, row.getUsername(), USERNAME_SIZE);
        
        // Write email (fixed-width field)
        writeFixedStringDirect(data, offset + EMAIL_OFFSET, row.getEmail(), EMAIL_SIZE);
        
        // Flush the page to disk
        pager.markPageDirty(pageNum);
        
        return true;
    }
    
    /**
     * Writes a string to a fixed-width field in a buffer.
     *
     * @param buffer The buffer to write to
     * @param str The string to write
     * @param fieldSize The size of the field
     */
    private void writeFixedStringDirect(byte[] data, int offset, String str, int fieldSize) {
        // Clear the field first
        for (int i = 0; i < fieldSize; i++) {
            data[offset + i] = 0;
        }
        
        // If string is null or empty, leave it zeroed
        if (str == null || str.isEmpty()) {
            return;
        }
        
        // Write the string bytes
        byte[] bytes = str.getBytes();
        int bytesToWrite = Math.min(bytes.length, fieldSize);
        System.arraycopy(bytes, 0, data, offset, bytesToWrite);
    }
    
    /**
     * Deserializes a row from the specified page and row offset.
     *
     * @param pageNum The page number to read from
     * @param rowOffset The row offset within the page
     * @return The deserialized row or null if an error occurs
     */
    public Row deserializeRowAt(int pageNum, int rowOffset) {
        try {
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
            
            byte[] data = page.getData();
            
            // Read ID - little-endian format
            int id = (data[offset] & 0xFF) |
                    ((data[offset + 1] & 0xFF) << 8) |
                    ((data[offset + 2] & 0xFF) << 16) |
                    ((data[offset + 3] & 0xFF) << 24);
            
            // Don't bother with the rest if ID is 0 (empty row)
            if (id <= 0) {
                return null;
            }
            
            // Read username
            String username = readFixedStringDirect(data, offset + USERNAME_OFFSET, USERNAME_SIZE);
            
            // Read email
            String email = readFixedStringDirect(data, offset + EMAIL_OFFSET, EMAIL_SIZE);
            
            return new Row(id, username, email);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error deserializing row", e);
            return null;
        }
    }
    
    /**
     * Reads a fixed-width string from a buffer.
     *
     * @param buffer The buffer to read from
     * @param fieldSize The size of the field
     * @return The string read from the buffer
     */
    private String readFixedStringDirect(byte[] data, int offset, int fieldSize) {
        // Find the end of the string (null terminator)
        int endPos = 0;
        while (endPos < fieldSize && data[offset + endPos] != 0) {
            endPos++;
        }
        
        if (endPos == 0) {
            return "";
        }
        
        return new String(data, offset, endPos).trim();
    }

    /**
     * Prints a row to the console.
     *
     * @param row The row to print
     */
    public void printRow(Row row) {
        if (row != null) {
            System.out.printf("(%d, %s, %s)%n", row.getId(), row.getUsername(), row.getEmail());
        }
    }
    
    /**
     * Get a row by ID using the index.
     * 
     * @param id The ID to look up
     * @return The row with the specified ID, or null if not found
     */
    public Row getRowById(int id) {
        lock.readLock().lock();
        try {
            Integer rowNum = idToRowNum.get(id);
            if (rowNum == null) {
                return null;
            }
            
            int pageNum = rowNum / Database.ROWS_PER_PAGE;
            int rowOffset = rowNum % Database.ROWS_PER_PAGE;
            
            return deserializeRowAt(pageNum, rowOffset);
        } finally {
            lock.readLock().unlock();
        }
    }
}