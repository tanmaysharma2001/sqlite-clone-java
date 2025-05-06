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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "select(?:\\s+(.+?))?(?:\\s+where\\s+(.+))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
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
        
        // Add single application-level shutdown hook
        addShutdownHook();
        
        try {
            // Validate the filename
            validateDatabaseFilename(filename);
    
            // Initialize the input buffer and open the database
            InputBuffer inputBuffer = new InputBuffer();
            Table table = null;
            
            try {
                // Open the database connection
                table = Database.open(filename);
                
                // Set up a scanner to read user input from the command line
                try (Scanner scanner = new Scanner(System.in)) {
                    // Display welcome message
                    printWelcome(filename);
                    
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
    
                            // Process the input
                            if (!processInput(inputBuffer, table)) {
                                break; // Exit requested
                            }
                            
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Error processing command", e);
                            System.err.println("Error: " + e.getMessage());
                        }
                    }
                }
            } finally {
                // Ensure database is closed properly
                if (table != null) {
                    Database.close(table);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error", e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add a single application-level shutdown hook for cleanup.
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\nShutting down database...");
                Database.closeAll();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during shutdown", e);
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }));
    }
    
    /**
     * Print welcome message.
     * 
     * @param filename The database filename
     */
    private static void printWelcome(String filename) {
        System.out.println("SQLite Clone v1.0");
        System.out.println("Enter \".help\" for usage hints.");
        System.out.println("Connected to " + filename);
    }
    
    /**
     * Process a user input command.
     * 
     * @param inputBuffer The input buffer containing the command
     * @param table The database table
     * @return False if exit was requested, true otherwise
     */
    private static boolean processInput(InputBuffer inputBuffer, Table table) {
        // Check if the input starts with a meta command (commands starting with '.')
        if (inputBuffer.getBuffer().startsWith(".")) {
            MetaCommandResult metaCommandResult = doMetaCommand(inputBuffer, table);
            
            if (metaCommandResult == MetaCommandResult.META_COMMAND_EXIT) {
                return false; // Exit requested
            }
            
            if (metaCommandResult == MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND) {
                System.out.printf("Unrecognized command '%s'.%n", inputBuffer.getBuffer());
                printHelp();
            }
            
            return true;
        }

        // Prepare the SQL statement for execution
        Statement statement = new Statement();
        
        PrepareResult prepareResult = prepareStatement(inputBuffer, statement);
        
        switch (prepareResult) {
            case PREPARE_SUCCESS:
                break;
            case PREPARE_SYNTAX_ERROR:
                System.out.println("Syntax error. Could not parse statement.");
                return true;
            case PREPARE_NEGATIVE_ID:
                System.out.println("ID must be positive.");
                return true;
            case PREPARE_UNRECOGNIZED_STATEMENT:
                System.out.printf("Unrecognized keyword at start of '%s'.%n", 
                                 inputBuffer.getBuffer());
                return true;
            default:
                System.out.println("Unknown prepare error.");
                return true;
        }

        ExecuteResult executeResult = executeStatement(statement, table);
        
        switch (executeResult) {
            case EXECUTE_SUCCESS:
                System.out.println("Executed.");
                break;
            case EXECUTE_TABLE_FULL:
                System.out.println("Error: Table full.");
                break;
            case EXECUTE_DUPLICATE_KEY:
                System.out.println("Error: Duplicate ID.");
                break;
            case EXECUTE_ROW_NOT_FOUND:
                System.out.println("Error: Row not found.");
                break;
            case EXECUTE_CONSTRAINT_VIOLATION:
                System.out.println("Error: Constraint violation.");
                break;
            default:
                System.out.println("Error: Execution failed.");
                break;
        }
        
        return true;
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
                // Exit is handled in main loop
                Database.close(table);
                System.out.println("Goodbye!");
                return MetaCommandResult.META_COMMAND_EXIT;
                
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
        System.out.println("  select [* | columns] [where condition]");
        System.out.println("    - Display rows in the table");
        System.out.println("    - Example: select where id = 1");
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
        System.out.println("  " + Database.getConnectionStats());
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
        
        if (input.toLowerCase().startsWith("select")) {
            return prepareSelect(buffer, statement);
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
            logger.log(Level.WARNING, "Invalid data for insert", e);
            System.err.println("Error: " + e.getMessage());
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }
    }
    
    /**
     * Prepares a SELECT statement.
     *
     * @param buffer The input buffer containing the statement
     * @param statement The statement to prepare
     * @return The result of the preparation
     */
    public static PrepareResult prepareSelect(InputBuffer buffer, Statement statement) {
        Matcher matcher = SELECT_PATTERN.matcher(buffer.getBuffer());
        
        if (!matcher.matches()) {
            return PrepareResult.PREPARE_SYNTAX_ERROR;
        }
        
        String columns = matcher.group(1);
        String whereClause = matcher.group(2);
        
        statement.setType(StatementType.STATEMENT_SELECT);
        
        // Parse columns if specified
        if (columns != null && !columns.trim().isEmpty() && !columns.equals("*")) {
            String[] columnArray = columns.split(",");
            for (int i = 0; i < columnArray.length; i++) {
                columnArray[i] = columnArray[i].trim();
            }
            statement.setSelectColumns(columnArray);
        }
        
        // Parse WHERE clause if specified
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            statement.setWhereClause(whereClause.trim());
        }
        
        return PrepareResult.PREPARE_SUCCESS;
    }

    /**
     * Executes a prepared statement.
     *
     * @param statement The statement to execute
     * @param table The database table
     * @return The result of the execution
     */
    public static ExecuteResult executeStatement(Statement statement, Table table) {
        try {
            switch (statement.getType()) {
                case STATEMENT_INSERT:
                    return table.executeInsert(statement);
                case STATEMENT_SELECT:
                    return table.executeSelect(statement);
                default:
                    return ExecuteResult.EXECUTE_FAIL;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing statement", e);
            System.err.println("Error: " + e.getMessage());
            return ExecuteResult.EXECUTE_FAIL;
        }
    }
}