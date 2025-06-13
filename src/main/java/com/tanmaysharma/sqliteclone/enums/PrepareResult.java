package com.tanmaysharma.sqliteclone.enums;

/**
 * Result codes for SQL statement preparation.
 * Indicates parsing and validation outcomes.
 */
public enum PrepareResult {
    PREPARE_SUCCESS("Statement prepared successfully"),
    PREPARE_SYNTAX_ERROR("Syntax error in statement"),
    PREPARE_UNRECOGNIZED_STATEMENT("Unrecognized statement type"),
    PREPARE_NEGATIVE_ID("ID must be positive"),
    PREPARE_INVALID_DATA("Invalid data provided"),
    PREPARE_TOO_LONG("Statement too long");

    private final String description;

    PrepareResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}