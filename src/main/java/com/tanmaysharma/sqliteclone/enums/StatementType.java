package com.tanmaysharma.sqliteclone.enums;

/**
 * Types of SQL statements supported by the database.
 */
public enum StatementType {
    STATEMENT_INSERT("INSERT"),
    STATEMENT_SELECT("SELECT"),
    STATEMENT_UPDATE("UPDATE"),
    STATEMENT_DELETE("DELETE");

    private final String keyword;

    StatementType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    /**
     * Gets the statement type from a keyword.
     *
     * @param keyword The SQL keyword
     * @return The corresponding statement type, or null if not found
     */
    public static StatementType fromKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        for (StatementType type : values()) {
            if (type.keyword.equalsIgnoreCase(keyword.trim())) {
                return type;
            }
        }
        return null;
    }
}