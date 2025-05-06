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

package com.tanmaysharma.sqliteclone;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.util.Arrays;

/**
 * Main class containing the database REPL (Read-Eval-Print Loop).
 * Entry point to the SQLite clone application.
 */
public class Main {
    private static final String PROMPT = "db > ";
    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "insert\\s+(\\d+)\\s+([^\\s]+)\\s+(.+)",
            Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // Ensure the database filename is supplied as a command line argument
        if (args.length < 1) {
            System.out.println("Must supply a database filename.");
            System.out.println("Usage: java -cp <classpath> com.tanmaysharma.sqliteclone.Main <database_file>");
            System.exit(1);
        }

        String filename = args[0];
        
        // Validate the filename
        validateDatabaseFilename(filename);

        // Initialize the input buffer and open the database
        InputBuffer inputBuffer = new InputBuffer();
        Table table = null;
        
        try {
            // Open the database connection
            table = Database.open(filename);
            
            // Add shutdown hook to ensure the database is closed properly
            final Table finalTable = table;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (finalTable != null) {
                        System.out.println("\nShutting down database...");
                        Database.close(finalTable);
                    }
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }));
            
            // Set up a scanner to read user input from the command line
            try (Scanner scanner = new Scanner(System.in)) {
                // Display welcome message
                System.out.println("SQLite Clone v1.0");
                System.out.println("Enter \".help\" for usage hints.");
                System.out.println("Connected to " + filename);
                
                // Infinite loop to keep the REPL running
                while (true) {
                    try {
                        // Print the prompt for user input
                        printPrompt();

                        // Read the input from the user
                        if (!scanner.hasNextLine()) {
                            break;  // End of input
                        }
                        
                        inputBuffer.readInput(scanner);
                        
                        // Skip empty input
                        if (inputBuffer.getBuffer().isEmpty()) {
                            continue;
                        }

                        // Check if the input starts with a meta command (commands starting with '.')
                        if (inputBuffer.getBuffer().startsWith(".")) {
                            MetaCommandResult metaCommandResult = doMetaCommand(inputBuffer, table);
                            if (metaCommandResult == MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND) {
                                System.out.printf("Unrecognized command '%s'.%n", inputBuffer.getBuffer());
                                printHelp();
                            }
                            continue;
                        }

                        // Prepare the SQL statement for execution
                        Statement statement = new Statement();
                        
                        PrepareResult prepareResult = prepareStatement(inputBuffer, statement);
                        
                        switch (prepareResult) {
                            case PREPARE_SUCCESS:
                                break;
                            case PREPARE_SYNTAX_ERROR:
                                System.out.println("Syntax error. Could not parse statement.");
                                continue;
                            case PREPARE_NEGATIVE_ID:
                                System.out.println("ID must be positive.");
                                continue;
                            case PREPARE_UNRECOGNIZED_STATEMENT:
                                System.out.printf("Unrecognized keyword at start of '%s'.%n", 
                                                 inputBuffer.getBuffer());
                                continue;
                            default:
                                System.out.println("Unknown prepare error.");
                                continue;
                        }

                        ExecuteResult executeResult = executeStatement(statement, table);
                        
                        switch (executeResult) {
                            case EXECUTE_SUCCESS:
                                System.out.println("Executed.");
                                break;
                            case EXECUTE_TABLE_FULL:
                                System.out.println("Error: Table full.");
                                break;
                            default:
                                System.out.println("Error: Execution failed.");
                                break;
                        }
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure database is closed properly
            if (table != null) {
                Database.close(table);
            }
        }
    }

    /**
     * Validates the database filename.
     *
     * @param filename The filename to validate
     * @throws IllegalArgumentException if the filename is invalid
     */
    private static void validateDatabaseFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Database filename cannot be empty");
        }
        
        File file = new File(filename);
        File parentDir = file.getParentFile();
        
        // Ensure parent directory exists or can be created
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory: " + parentDir);
            }
        }
        
        // Check if file can be created or is writable
        if (file.exists() && !file.canWrite()) {
            throw new IllegalArgumentException("Cannot write to file: " + filename);
        }
    }

    /**
     * Prints the command prompt.
     */
    public static void printPrompt() {
        System.out.print(PROMPT);
    }

    /**
     * Handles meta commands that start with a dot.
     *
     * @param buffer The input buffer containing the command
     * @param table The database table
     * @return The result of the meta command
     */
    public static MetaCommandResult doMetaCommand(InputBuffer buffer, Table table) {
        String command = buffer.getBuffer().toLowerCase();
        
        switch (command) {
            case ".exit":
                // Exit is handled in main loop via Scanner breaking
                Database.close(table);
                System.out.println("Goodbye!");
                System.exit(0);
                return MetaCommandResult.META_COMMAND_SUCCESS;
                
            case ".help":
                printHelp();
                return MetaCommandResult.META_COMMAND_SUCCESS;
                
            case ".stats":
                printStats(table);
                return MetaCommandResult.META_COMMAND_SUCCESS;
                
            default:
                return MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND;
        }
    }

    /**
     * Prints help information.
     */
    private static void printHelp() {
        System.out.println("Special commands:");
        System.out.println("  .help                - Display this help message");
        System.out.println("  .exit                - Exit the program");
        System.out.println("  .stats               - Display database statistics");
        System.out.println();
        System.out.println("SQL commands:");
        System.out.println("  insert <id> <username> <email>");
        System.out.println("    - Insert a new row with the specified values");
        System.out.println("  select");
        System.out.println("    - Display all rows in the table");
    }

    /**
     * Prints database statistics.
     *
     * @param table The database table
     */
    private static void printStats(Table table) {
        System.out.println("Database statistics:");
        System.out.println("  Rows: " + table.getNumRows());
        System.out.println("  Page size: " + Database.PAGE_SIZE + " bytes");
        System.out.println("  Row size: " + Database.ROW_SIZE + " bytes");
        System.out.println("  Max pages: " + Database.MAX_PAGES);
        System.out.println("  Rows per page: " + Database.ROWS_PER_PAGE);
    }

    /**
     * Prepares a statement for execution.
     *
     * @param buffer The input buffer containing the statement
     * @param statement The statement to prepare
     * @return The result of the preparation
     */
    public static PrepareResult prepareStatement(InputBuffer buffer, Statement statement) {
        String input = buffer.getBuffer().trim();
        
        if (input.toLowerCase().startsWith("insert")) {
            return prepareInsert(buffer, statement);
        }
        
        if (input.toLowerCase().equals("select")) {
            statement.setType(StatementType.STATEMENT_SELECT);
            return PrepareResult.PREPARE_SUCCESS;
        }
        
        return PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT;
    }

    /**
     * Prepares an INSERT statement.
     *
     * @param buffer The input buffer containing the statement
     * @param statement The statement to prepare
     * @return The result of the preparation
     */
    public static PrepareResult prepareInsert(InputBuffer buffer, Statement statement) {
        Matcher matcher = INSERT_PATTERN.matcher(buffer.getBuffer());
        
        if (!matcher.matches()) {
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }

        try {
            int id = Integer.parseInt(matcher.group(1));
            if (id < 0) {
                return PrepareResult.PREPARE_NEGATIVE_ID;
            }

            String username = matcher.group(2);
            String email = matcher.group(3);
            
            // Validate data
            if (username.length() > Row.MAX_USERNAME_LENGTH) {
                System.out.println("Warning: Username exceeds maximum length and will be truncated.");
            }
            
            if (email.length() > Row.MAX_EMAIL_LENGTH) {
                System.out.println("Warning: Email exceeds maximum length and will be truncated.");
            }
            
            // Create row
            Row row = new Row(id, username, email);
            statement.setRowToInsert(row);
            statement.setType(StatementType.STATEMENT_INSERT);
            
            return PrepareResult.PREPARE_SUCCESS;
        } catch (NumberFormatException e) {
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }
    }

    /**
     * Executes a prepared statement.
     *
     * @param statement The statement to execute
     * @param table The database table
     * @return The result of the execution
     */
    public static ExecuteResult executeStatement(Statement statement, Table table) {
        switch (statement.getType()) {
            case STATEMENT_INSERT:
                return table.executeInsert(statement);
            case STATEMENT_SELECT:
                return table.executeSelect(statement);
            default:
                return ExecuteResult.EXECUTE_FAIL;
        }
    }
}