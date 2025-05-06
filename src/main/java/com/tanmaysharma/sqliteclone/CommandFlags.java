package com.tanmaysharma.sqliteclone;

public class CommandFlags {

    // Enum for ExecuteResult
    public enum ExecuteResult {
        SUCCESS,
        TABLE_FULL,
        FAILED
    }

    // Enum for MetaCommandResult
    public enum MetaCommandResult {
        SUCCESS,
        UNRECOGNIZED_COMMAND
    }

    // Enum for PrepareResult
    public enum PrepareResult {
        SUCCESS,
        UNRECOGNIZED_STATEMENT,
        SYNTAX_ERROR,
        NEGATIVE_ID
    }

    // Enum for StatementType
    public enum StatementType {
        INSERT,
        SELECT
    }
}
