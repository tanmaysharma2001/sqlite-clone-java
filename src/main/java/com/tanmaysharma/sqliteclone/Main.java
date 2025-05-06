package com.tanmaysharma.sqliteclone;

import java.util.Scanner;

// RUN REPL:
// mvn exec:java -- <path-to-your-.db-file>
// mvn exec:java -- test.db

// create a standalone jar:
// mvn package
// then run:
// java -cp target/sqlite-clone-java-1.0-SNAPSHOT.jar \
//      com.tanmaysharma.sqliteclone.Main test.db

// running commands:
// insert 1 alice alice@example.com
// select
// .exit

// db > insert 1 alice alice@example.com
// Executed
// db > select
// (1, alice, alice@example.com)
// db > .exit

public class Main {
    public static void main(String[] args) {
        // Ensure the database filename is supplied as a command line argument
        if (args.length < 1) {
            System.out.println("Must supply a database filename.");
            System.exit(1);
        }

        String filename = args[0];

        // Initialize the input buffer and open the database
        InputBuffer inputBuffer = new InputBuffer();
        Table table = Database.open(filename);

        // Set up a scanner to read user input from the command line
        Scanner scanner = new Scanner(System.in);

        // Infinite loop to keep the REPL running
        while (true) {
            // Print the prompt for user input
            printPrompt();

            // Read the input from the user
            inputBuffer.readInput(scanner);

            // Check if the input starts with a meta command (commands starting with '.')
            if (inputBuffer.buffer.startsWith(".")) {
                MetaCommandResult metaCommandResult = doMetaCommand(inputBuffer, table);
                if (metaCommandResult == MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND) {
                    System.out.printf("Unrecognized command '%s'.%n", inputBuffer.buffer);
                }
                continue;
            }

            // Prepare the SQL statement for execution
            Statement statement = new Statement();
            
            PrepareResult prepareResult = prepareStatement(inputBuffer, statement);
            
            if (prepareResult != PrepareResult.PREPARE_SUCCESS) {
                System.out.println("Error preparing statement.");
                continue;
            }

            ExecuteResult executeResult = executeStatement(statement, table);
            
            if (executeResult == ExecuteResult.EXECUTE_SUCCESS) {
                System.out.println("Executed");
            } else {
                System.out.println("Execution failed.");
            }
        }
    }

    // Function to print the prompt for the REPL
    public static void printPrompt() {
        System.out.print("db > ");
    }

    public static MetaCommandResult doMetaCommand(InputBuffer buffer, Table table) {
        if (buffer.buffer.equals(".exit")) {
            Database.close(table);
            System.exit(0);
            return MetaCommandResult.META_COMMAND_SUCCESS;
        }
        return MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND;
    }

    public static PrepareResult prepareStatement(InputBuffer buffer, Statement statement) {
        String[] arguments = buffer.buffer.split(" ");
        if (arguments[0].equals("insert")) {
            return prepareInsert(buffer, statement);
        }
        if (arguments[0].equals("select")) {
            statement.setType(StatementType.STATEMENT_SELECT);
            return PrepareResult.PREPARE_SUCCESS;
        }
        return PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT;
    }

    public static PrepareResult prepareInsert(InputBuffer buffer, Statement statement) {
        String[] arguments = buffer.buffer.split(" ");
        if (arguments.length < 4) {
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }

        try {
            int id = Integer.parseInt(arguments[1]);
            if (id < 0) {
                return PrepareResult.PREPARE_NEGATIVE_ID;
            }

            Row row = new Row(id, arguments[2], arguments[3]);
            statement.setRowToInsert(row);
            statement.setType(StatementType.STATEMENT_INSERT);
            return PrepareResult.PREPARE_SUCCESS;
        } catch (NumberFormatException e) {
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }
    }

    public static ExecuteResult executeStatement(Statement statement, Table table) {
        if (statement.getType() == StatementType.STATEMENT_INSERT) {
            return table.executeInsert(statement);
        } else if (statement.getType() == StatementType.STATEMENT_SELECT) {
            return table.executeSelect(statement);
        }
        return ExecuteResult.EXECUTE_FAIL;
    }

}
