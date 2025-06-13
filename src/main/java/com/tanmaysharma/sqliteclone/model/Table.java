package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import com.tanmaysharma.sqliteclone.util.ValidationUtil;

import java.util.Objects;

/**
 * Represents a database table with metadata and configuration.
 * Immutable data structure containing table information.
 */
public final class Table {
    private final String name;
    private final int maxRows;
    private final int rowSize;
    private final int pageSize;
    private final int rowsPerPage;
    private final long createdAt;

    /**
     * Creates a new table with the specified name.
     *
     * @param name The table name
     * @throws ValidationException if the name is invalid
     */
    public Table(String name) {
        this(name, DatabaseConfig.TABLE_MAX_ROWS, DatabaseConfig.ROW_SIZE,
                DatabaseConfig.PAGE_SIZE, System.currentTimeMillis());
    }

    /**
     * Creates a new table with full configuration.
     *
     * @param name The table name
     * @param maxRows The maximum number of rows
     * @param rowSize The size of each row in bytes
     * @param pageSize The size of each page in bytes
     * @param createdAt The creation timestamp
     * @throws ValidationException if any parameter is invalid
     */
    public Table(String name, int maxRows, int rowSize, int pageSize, long createdAt) {
        ValidationUtil.requireNonEmpty(name, "table name");
        ValidationUtil.requirePositive(maxRows, "maxRows");
        ValidationUtil.requirePositive(rowSize, "rowSize");
        ValidationUtil.requirePositive(pageSize, "pageSize");

        if (pageSize < rowSize) {
            throw new ValidationException(
                    "Page size must be at least as large as row size",
                    "pageSize", pageSize
            );
        }

        this.name = name.trim();
        this.maxRows = maxRows;
        this.rowSize = rowSize;
        this.pageSize = pageSize;
        this.rowsPerPage = pageSize / rowSize;
        this.createdAt = createdAt;

        if (this.rowsPerPage == 0) {
            throw new ValidationException(
                    "Row size is too large for page size - no rows fit in a page",
                    "rowSize", rowSize
            );
        }
    }

    /**
     * Gets the table name.
     *
     * @return The table name
     */
    public String getName() {
        return name;
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
     * Gets the size of each row in bytes.
     *
     * @return The row size
     */
    public int getRowSize() {
        return rowSize;
    }

    /**
     * Gets the size of each page in bytes.
     *
     * @return The page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Gets the number of rows that fit in each page.
     *
     * @return The rows per page
     */
    public int getRowsPerPage() {
        return rowsPerPage;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return The creation timestamp in milliseconds
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Calculates the maximum number of pages needed.
     *
     * @return The maximum page count
     */
    public int getMaxPages() {
        return (maxRows + rowsPerPage - 1) / rowsPerPage;
    }

    /**
     * Calculates which page a row at the given index would be on.
     *
     * @param rowIndex The row index
     * @return The page number
     * @throws IllegalArgumentException if the row index is invalid
     */
    public int getPageForRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= maxRows) {
            throw new IllegalArgumentException("Row index out of bounds: " + rowIndex);
        }
        return rowIndex / rowsPerPage;
    }

    /**
     * Calculates the offset within a page for a row at the given index.
     *
     * @param rowIndex The row index
     * @return The row offset within the page
     * @throws IllegalArgumentException if the row index is invalid
     */
    public int getRowOffsetInPage(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= maxRows) {
            throw new IllegalArgumentException("Row index out of bounds: " + rowIndex);
        }
        return rowIndex % rowsPerPage;
    }

    /**
     * Creates a builder for constructing tables.
     *
     * @return A new table builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for creating tables.
     */
    public static class Builder {
        private String name;
        private int maxRows = DatabaseConfig.TABLE_MAX_ROWS;
        private int rowSize = DatabaseConfig.ROW_SIZE;
        private int pageSize = DatabaseConfig.PAGE_SIZE;
        private long createdAt = System.currentTimeMillis();

        /**
         * Sets the table name.
         *
         * @param name The table name
         * @return This builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the maximum number of rows.
         *
         * @param maxRows The maximum row count
         * @return This builder for chaining
         */
        public Builder maxRows(int maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        /**
         * Sets the row size.
         *
         * @param rowSize The row size in bytes
         * @return This builder for chaining
         */
        public Builder rowSize(int rowSize) {
            this.rowSize = rowSize;
            return this;
        }

        /**
         * Sets the page size.
         *
         * @param pageSize The page size in bytes
         * @return This builder for chaining
         */
        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt The creation timestamp
         * @return This builder for chaining
         */
        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds a new table with the set parameters.
         *
         * @return The constructed table
         * @throws ValidationException if the parameters are invalid
         */
        public Table build() {
            return new Table(name, maxRows, rowSize, pageSize, createdAt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return maxRows == table.maxRows &&
                rowSize == table.rowSize &&
                pageSize == table.pageSize &&
                createdAt == table.createdAt &&
                Objects.equals(name, table.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maxRows, rowSize, pageSize, createdAt);
    }

    @Override
    public String toString() {
        return String.format(
                "Table{name='%s', maxRows=%d, rowSize=%d, pageSize=%d, rowsPerPage=%d, createdAt=%d}",
                name, maxRows, rowSize, pageSize, rowsPerPage, createdAt
        );
    }
}