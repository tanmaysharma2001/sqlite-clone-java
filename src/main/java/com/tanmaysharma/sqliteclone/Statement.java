package com.tanmaysharma.sqliteclone;

/**
 * Represents a SQL statement to be executed.
 * Contains statement type and associated data.
 */
public class Statement {
    private StatementType type;
    private Row rowToInsert;
    private String whereClause;
    private String[] selectColumns;

    /**
     * Creates a new statement with default values.
     */
    public Statement() {
        this.type = null;
        this.rowToInsert = null;
        this.whereClause = null;
        this.selectColumns = null;
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
     * Sets the statement type.
     *
     * @param type The statement type to set
     */
    public void setType(StatementType type) {
        this.type = type;
    }

    /**
     * Gets the row to insert.
     *
     * @return The row to insert
     */
    public Row getRowToInsert() {
        return rowToInsert;
    }

    /**
     * Sets the row to insert.
     *
     * @param rowToInsert The row to insert
     */
    public void setRowToInsert(Row rowToInsert) {
        this.rowToInsert = rowToInsert;
    }
    
    /**
     * Gets the WHERE clause.
     *
     * @return The WHERE clause
     */
    public String getWhereClause() {
        return whereClause;
    }
    
    /**
     * Sets the WHERE clause.
     *
     * @param whereClause The WHERE clause to set
     */
    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }
    
    /**
     * Gets the columns to select.
     *
     * @return The columns to select
     */
    public String[] getSelectColumns() {
        return selectColumns;
    }
    
    /**
     * Sets the columns to select.
     *
     * @param selectColumns The columns to select
     */
    public void setSelectColumns(String[] selectColumns) {
        this.selectColumns = selectColumns;
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
     * Builder pattern for creating statements.
     */
    public static class Builder {
        private StatementType type;
        private Row rowToInsert;
        private String whereClause;
        private String[] selectColumns;
        
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
         * Builds a new statement with the set parameters.
         *
         * @return The constructed statement
         */
        public Statement build() {
            Statement statement = new Statement();
            statement.setType(type);
            statement.setRowToInsert(rowToInsert);
            statement.setWhereClause(whereClause);
            statement.setSelectColumns(selectColumns);
            return statement;
        }
    }
}