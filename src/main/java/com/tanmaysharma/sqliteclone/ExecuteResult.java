package com.tanmaysharma.sqliteclone;

/**
 * Result codes for SQL statement execution.
 */
public enum ExecuteResult {
    EXECUTE_SUCCESS,
    EXECUTE_TABLE_FULL,
    EXECUTE_DUPLICATE_KEY,
    EXECUTE_ROW_NOT_FOUND,
    EXECUTE_PERMISSION_DENIED,
    EXECUTE_CONSTRAINT_VIOLATION,
    EXECUTE_FAIL
}