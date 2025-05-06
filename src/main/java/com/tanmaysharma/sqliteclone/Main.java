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
            inputBuffer.setBuffer(scanner.nextLine().trim());

            // Check if the input starts with a meta command (commands starting with '.')
            if (inputBuffer.getBuffer().startsWith(".")) {
                MetaCommandResult metaCommandResult = MetaCommands.doMetaCommand(inputBuffer, table);
                if (metaCommandResult == MetaCommandResult.SUCCESS) {
                    continue;
                } else {
                    System.out.printf("Unrecognized command '%s'.%n", inputBuffer.getBuffer());
                    continue;
                }
            }

            // Prepare the SQL statement for execution
            Statement statement = new Statement();
            PrepareResult prepareResult = Statement.prepareStatement(inputBuffer, statement);

            // Handle the result of preparing the statement
            switch (prepareResult) {
                case SUCCESS:
                    break;
                case SYNTAX_ERROR:
                    System.out.println("Syntax error could not parse statement.");
                    continue;
                case UNRECOGNIZED_STATEMENT:
                    System.out.printf("Unrecognized keyword at start of '%s'.%n", inputBuffer.getBuffer());
                    continue;
                case NEGATIVE_ID:
                    System.out.println("Parsing Error: Negative ID passed");
                    continue;
            }

            // Execute the SQL statement
            ExecuteResult executeResult = table.executeStatement(statement);

            // Handle the result of executing the statement
            switch (executeResult) {
                case SUCCESS:
                    System.out.println("Executed");
                    break;
                case TABLE_FULL:
                    System.out.println("Table is full!");
                    break;
                case FAILED:
                    System.out.println("Executing the command failed.");
                    break;
            }
        }
    }

    // Function to print the prompt for the REPL
    private static void printPrompt() {
        System.out.print("db > ");
    }
}
