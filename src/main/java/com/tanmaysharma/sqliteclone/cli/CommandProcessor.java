package com.tanmaysharma.sqliteclone.cli;

import com.tanmaysharma.sqliteclone.config.ApplicationConfig;
import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.enums.ExecuteResult;
import com.tanmaysharma.sqliteclone.enums.MetaCommandResult;
import com.tanmaysharma.sqliteclone.exception.QueryException;
import com.tanmaysharma.sqliteclone.query.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes commands from the command-line interface.
 * Handles both meta commands and SQL commands.
 */
public class CommandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CommandProcessor.class);

    private final QueryEngine queryEngine;
    private final String databaseFilename;

    /**
     * Creates a new command processor.
     *
     * @param queryEngine The query engine to use
     * @param databaseFilename The database filename
     */
    public CommandProcessor(QueryEngine queryEngine, String databaseFilename) {
        this.queryEngine = queryEngine;
        this.databaseFilename = databaseFilename;
    }

    /**
     * Processes a meta command (commands starting with dot).
     *
     * @param command The meta command
     * @return The result of processing the command
     */
    public MetaCommandResult processMetaCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();

        switch (lowerCommand) {
            case ".exit":
                System.out.println("Goodbye!");
                return MetaCommandResult.META_COMMAND_EXIT;

            case ".help":
                System.out.println(ApplicationConfig.HELP_MESSAGE);
                return MetaCommandResult.META_COMMAND_SUCCESS;

            case ".stats":
                printStatistics();
                return MetaCommandResult.META_COMMAND_SUCCESS;

            case ".info":
                printDatabaseInfo();
                return MetaCommandResult.META_COMMAND_SUCCESS;

            case ".clear":
                clearScreen();
                return MetaCommandResult.META_COMMAND_SUCCESS;

            default:
                return MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND;
        }
    }

    /**
     * Processes a SQL command.
     *
     * @param command The SQL command
     * @throws QueryException if the command fails
     */
    public void processSqlCommand(String command) throws QueryException {
        try {
            ExecuteResult result = queryEngine.executeQuery(command);

            switch (result) {
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
                case EXECUTE_TIMEOUT:
                    System.out.println("Error: Query timeout.");
                    break;
                default:
                    System.out.println("Error: " + result.getDescription());
                    break;
            }

        } catch (QueryException e) {
            logger.error("Query execution failed: {}", command, e);
            throw e;
        }
    }

    /**
     * Prints database statistics.
     */
    private void printStatistics() {
        System.out.println("Database Statistics:");
        System.out.println("  File: " + databaseFilename);
        System.out.println("  Page size: " + DatabaseConfig.PAGE_SIZE + " bytes");
        System.out.println("  Row size: " + DatabaseConfig.ROW_SIZE + " bytes");
        System.out.println("  Max pages: " + DatabaseConfig.MAX_PAGES);
        System.out.println("  Rows per page: " + DatabaseConfig.ROWS_PER_PAGE);
        System.out.println("  Max rows: " + DatabaseConfig.TABLE_MAX_ROWS);
        System.out.println();
        System.out.println(queryEngine.getQueryStatistics());
    }

    /**
     * Prints database information.
     */
    private void printDatabaseInfo() {
        System.out.println("Database Information:");
        System.out.println("  Application: " + ApplicationConfig.APPLICATION_NAME);
        System.out.println("  Version: " + ApplicationConfig.APPLICATION_VERSION);
        System.out.println("  Database file: " + databaseFilename);
        System.out.println("  Configuration: " + DatabaseConfig.getConfigurationSummary());
    }

    /**
     * Clears the screen (attempts to).
     */
    private void clearScreen() {
        try {
            // Try to clear screen using ANSI escape codes
            System.out.print("\033[2J\033[H");
            System.out.flush();
        } catch (Exception e) {
            // If that doesn't work, just print some newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
}