package com.tanmaysharma.sqliteclone.enums;

/**
 * Result codes for SQL statement execution.
 * Provides specific feedback on execution outcomes.
 */
public enum ExecuteResult {
  EXECUTE_SUCCESS("Command executed successfully"),
  EXECUTE_TABLE_FULL("Table has reached maximum capacity"),
  EXECUTE_DUPLICATE_KEY("Duplicate key violation"),
  EXECUTE_ROW_NOT_FOUND("Requested row not found"),
  EXECUTE_PERMISSION_DENIED("Permission denied"),
  EXECUTE_CONSTRAINT_VIOLATION("Constraint violation"),
  EXECUTE_TIMEOUT("Execution timeout"),
  EXECUTE_FAIL("Execution failed");

  private final String description;

  ExecuteResult(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}