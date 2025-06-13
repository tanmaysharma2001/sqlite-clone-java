package com.tanmaysharma.sqliteclone.exception;

/**
 * Exception thrown for query-related errors.
 * Includes parsing, validation, and execution errors.
 */
public class QueryException extends DatabaseException {

  /**
   * Creates a new query exception with the specified message.
   *
   * @param message The error message
   */
  public QueryException(String message) {
    super(message, "QUERY_ERROR");
  }

  /**
   * Creates a new query exception with the specified message and cause.
   *
   * @param message The error message
   * @param cause The cause of the exception
   */
  public QueryException(String message, Throwable cause) {
    super(message, cause, "QUERY_ERROR");
  }

  /**
   * Creates a new query exception with the specified message and error code.
   *
   * @param message The error message
   * @param errorCode The specific query error code
   */
  public QueryException(String message, String errorCode) {
    super(message, errorCode);
  }
}