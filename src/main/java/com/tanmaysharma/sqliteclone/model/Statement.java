package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.enums.StatementType;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a SQL statement to be executed.
 * Immutable data structure containing statement type and associated data.
 */
public final class Statement {
    private final StatementType type;
    private final Row rowToInsert;
    private final String whereClause;
    private final String[] selectColumns;
    private final String originalQuery;

    /**
     * Creates a new statement.
     *
     * @param type The statement type
     * @param rowToInsert The row to insert (for INSERT statements)
     * @param whereClause The WHERE clause (for SELECT statements)
     * @param selectColumns The columns to select (for SELECT statements)
     * @param originalQuery The original query string
     */
    private Statement(StatementType type, Row rowToInsert, String whereClause,
                      String[] selectColumns, String originalQuery) {
        this.type = type;
        this.rowToInsert = rowToInsert;
        this.whereClause = whereClause;
        this.selectColumns = selectColumns != null ? selectColumns.clone() : null;
        this.originalQuery = originalQuery;
    }

    /**
     * Gets the statement type.
     *
     * @return The statement type
     */
    public StatementType getType() {
        return type;
    }

    /**
     * Gets the row to insert.
     *
     * @return The row to insert, or null if not an INSERT statement
     */
    public Row getRowToInsert() {
        return rowToInsert;
    }

    /**
     * Gets the WHERE clause.
     *
     * @return The WHERE clause, or null if not specified
     */
    public String getWhereClause() {
        return whereClause;
    }

    /**
     * Gets the columns to select.
     *
     * @return The columns to select, or null for SELECT *
     */
    public String[] getSelectColumns() {
        return selectColumns != null ? selectColumns.clone() : null;
    }

    /**
     * Gets the original query string.
     *
     * @return The original query string
     */
    public String getOriginalQuery() {
        return originalQuery;
    }

    /**
     * Checks if this is an INSERT statement.
     *
     * @return True if INSERT statement
     */
    public boolean isInsert() {
        return type == StatementType.STATEMENT_INSERT;
    }

    /**
     * Checks if this is a SELECT statement.
     *
     * @return True if SELECT statement
     */
    public boolean isSelect() {
        return type == StatementType.STATEMENT_SELECT;
    }

    /**
     * Checks if this SELECT statement has a WHERE clause.
     *
     * @return True if has WHERE clause
     */
    public boolean hasWhereClause() {
        return whereClause != null && !whereClause.trim().isEmpty();
    }

    /**
     * Checks if this SELECT statement selects specific columns.
     *
     * @return True if specific columns are selected
     */
    public boolean hasSpecificColumns() {
        return selectColumns != null && selectColumns.length > 0;
    }

    /**
     * Creates a builder for constructing statements.
     *
     * @return A new statement builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an INSERT statement.
     *
     * @param row The row to insert
     * @param originalQuery The original query string
     * @return A new INSERT statement
     */
    public static Statement insertStatement(Row row, String originalQuery) {
        return new Statement(StatementType.STATEMENT_INSERT, row, null, null, originalQuery);
    }

    /**
     * Creates a SELECT statement.
     *
     * @param whereClause The WHERE clause (optional)
     * @param selectColumns The columns to select (optional)
     * @param originalQuery The original query string
     * @return A new SELECT statement
     */
    public static Statement selectStatement(String whereClause, String[] selectColumns, String originalQuery) {
        return new Statement(StatementType.STATEMENT_SELECT, null, whereClause, selectColumns, originalQuery);
    }

    /**
     * Builder pattern for creating statements.
     */
    public static class Builder {
        private StatementType type;
        private Row rowToInsert;
        private String whereClause;
        private String[] selectColumns;
        private String originalQuery;

        /**
         * Sets the statement type.
         *
         * @param type The statement type
         * @return This builder for chaining
         */
        public Builder type(StatementType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the row to insert.
         *
         * @param row The row to insert
         * @return This builder for chaining
         */
        public Builder rowToInsert(Row row) {
            this.rowToInsert = row;
            return this;
        }

        /**
         * Sets the WHERE clause.
         *
         * @param whereClause The WHERE clause
         * @return This builder for chaining
         */
        public Builder whereClause(String whereClause) {
            this.whereClause = whereClause;
            return this;
        }

        /**
         * Sets the columns to select.
         *
         * @param selectColumns The columns to select
         * @return This builder for chaining
         */
        public Builder selectColumns(String[] selectColumns) {
            this.selectColumns = selectColumns;
            return this;
        }

        /**
         * Sets the original query string.
         *
         * @param originalQuery The original query
         * @return This builder for chaining
         */
        public Builder originalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
            return this;
        }

        /**
         * Builds a new statement with the set parameters.
         *
         * @return The constructed statement
         * @throws IllegalStateException if required parameters are missing
         */
        public Statement build() {
            if (type == null) {
                throw new IllegalStateException("Statement type is required");
            }

            return new Statement(type, rowToInsert, whereClause, selectColumns, originalQuery);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statement statement = (Statement) o;
        return type == statement.type &&
                Objects.equals(rowToInsert, statement.rowToInsert) &&
                Objects.equals(whereClause, statement.whereClause) &&
                Arrays.equals(selectColumns, statement.selectColumns) &&
                Objects.equals(originalQuery, statement.originalQuery);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, rowToInsert, whereClause, originalQuery);
        result = 31 * result + Arrays.hashCode(selectColumns);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Statement{type=%s, rowToInsert=%s, whereClause='%s', " +
                        "selectColumns=%s, originalQuery='%s'}",
                type, rowToInsert, whereClause,
                Arrays.toString(selectColumns), originalQuery);
    }
}