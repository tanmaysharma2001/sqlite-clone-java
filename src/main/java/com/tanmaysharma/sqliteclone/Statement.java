package com.tanmaysharma.sqliteclone;

import java.util.Arrays;

public class Statement {
    public CommandFlags.StatementType type;
    public Table.Row rowToInsert;

    // MetaCommand handling function (like .exit command)
    public static CommandFlags.MetaCommandResult doMetaCommand(InputBuffer buffer, Table table) {
        if (buffer.getBuffer().equals(".exit")) {
            try {
                table.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
            return CommandFlags.MetaCommandResult.SUCCESS;
        } else {
            return CommandFlags.MetaCommandResult.UNRECOGNIZED_COMMAND;
        }
    }

    // Prepare INSERT statement
    public static CommandFlags.PrepareResult prepareInsert(InputBuffer buffer, Statement statement) {
        String[] arguments = buffer.getBuffer().split("\\s+");

        statement.type = CommandFlags.StatementType.INSERT;

        if (arguments.length < 4) {
            return CommandFlags.PrepareResult.SYNTAX_ERROR;
        }

        // Check for empty fields
        if (arguments[1].isBlank() || arguments[2].isBlank() || arguments[3].isBlank()) {
            return CommandFlags.PrepareResult.SYNTAX_ERROR;
        }

        int rowID;
        try {
            rowID = Integer.parseInt(arguments[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error parsing the ID field");
            return CommandFlags.PrepareResult.SYNTAX_ERROR;
        }

        if (rowID < 0) {
            return CommandFlags.PrepareResult.NEGATIVE_ID;
        }

        Table.Row row = new Table.Row(rowID, arguments[2], arguments[3]);
        statement.rowToInsert = row;

        return CommandFlags.PrepareResult.SUCCESS;
    }

    // Prepare a statement (INSERT or SELECT)
    public static CommandFlags.PrepareResult prepareStatement(InputBuffer buffer, Statement statement) {
        String[] arguments = buffer.getBuffer().split("\\s+");

        if (arguments[0].equals("insert")) {
            return prepareInsert(buffer, statement);
        }

        if (arguments[0].equals("select")) {
            statement.type = CommandFlags.StatementType.SELECT;
            return CommandFlags.PrepareResult.SUCCESS;
        }

        return CommandFlags.PrepareResult.UNRECOGNIZED_STATEMENT;
    }

    // Execute an INSERT statement
    public ExecuteResult executeInsert(Statement statement, Table table) {
        if (table.numRows >= Table.TABLE_MAX_ROWS) {
            return ExecuteResult.TABLE_FULL;
        } else {
            Table.Row rowToInsert = statement.rowToInsert;

            Cursor cursor = table.table_end();

            int currentPage, currentRow;
            currentPage = currentRow = table.cursorValue(cursor);

            try {
                table.serializeRow(rowToInsert, currentPage, currentRow);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return ExecuteResult.SUCCESS;
        }
    }

    // Execute a SELECT statement
    public ExecuteResult executeSelect(Statement statement, Table table) {
        Cursor cursor = table.table_start();

        while (!cursor.end_of_table) {
            Table.Row row = table.deserializeRow(table.cursorValue(cursor));
            Table.printRow(row);
            cursor.advance();
        }
        return ExecuteResult.SUCCESS;
    }

    // Execute a statement (insert or select)
    public static ExecuteResult executeStatement(Statement statement, Table table) {
        switch (statement.type) {
            case INSERT:
                return statement.executeInsert(statement, table);
            case SELECT:
                return statement.executeSelect(statement, table);
            default:
                return ExecuteResult.FAILED;
        }
    }
}
