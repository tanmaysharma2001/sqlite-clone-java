package com.tanmaysharma.sqliteclone.cli;

import com.tanmaysharma.sqliteclone.config.ApplicationConfig;
import com.tanmaysharma.sqliteclone.enums.MetaCommandResult;
import com.tanmaysharma.sqliteclone.exception.DatabaseException;
import com.tanmaysharma.sqliteclone.query.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Command-line interface for the SQLite clone.
 * Manages user interaction and command processing.
 */
public class CommandLineInterface {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineInterface.class);

    private final QueryEngine queryEngine;
    private final CommandProcessor commandProcessor;
    private final InputBuffer inputBuffer;
    private final String databaseFilename;
    private Scanner scanner;
    private boolean running = false;

    /**
     * Creates a new command-line interface.
     *
     * @param queryEngine The query engine to use
     * @param databaseFilename The database filename
     */
    public CommandLineInterface(QueryEngine queryEngine, String databaseFilename) {
        this.queryEngine = queryEngine;
        this.databaseFilename = databaseFilename;
        this.commandProcessor = new CommandProcessor(queryEngine, databaseFilename);
        this.inputBuffer = new InputBuffer();

        logger.info("CLI initialized for database: {}", databaseFilename);
    }

    /**
     * Starts the command-line interface.
     */
    public void start() {
        running = true;
        scanner = new Scanner(System.in);

        try {
            // Main REPL loop
            while (running) {
                try {
                    // Print prompt
                    printPrompt();

                    // Read input
                    if (!scanner.hasNextLine()) {
                        break; // End of input
                    }

                    inputBuffer.readInput(scanner);

                    // Skip empty input
                    if (inputBuffer.isEmpty()) {
                        continue;
                    }

                    // Process the command
                    if (!processCommand()) {
                        break; // Exit requested
                    }

                } catch (Exception e) {
                    logger.error("Error processing command", e);
                    System.err.println("Error: " + e.getMessage());
                }
            }

        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        logger.info("CLI session ended");
    }

    /**
     * Shuts down the command-line interface.
     */
    public void shutdown() {
        running = false;
        if (scanner != null) {
            scanner.close();
        }
        logger.info("CLI shutdown complete");
    }

    /**
     * Prints the command prompt.
     */
    private void printPrompt() {
        System.out.print(ApplicationConfig.CLI_PROMPT);
    }

    /**
     * Processes a command from the input buffer.
     *
     * @return False if exit was requested, true otherwise
     */
    private boolean processCommand() {
        String command = inputBuffer.getBuffer();

        try {
            if (command.startsWith(".")) {
                // Meta command
                MetaCommandResult result = commandProcessor.processMetaCommand(command);

                switch (result) {
                    case META_COMMAND_EXIT:
                        return false; // Exit requested
                    case META_COMMAND_UNRECOGNIZED_COMMAND:
                        System.out.printf("Unrecognized command '%s'.%n", command);
                        printHelp();
                        break;
                    case META_COMMAND_SUCCESS:
                        // Command processed successfully
                        break;
                }
            } else {
                // SQL command
                commandProcessor.processSqlCommand(command);
            }

            return true;

        } catch (DatabaseException e) {
            System.err.println("Database error: " + e.getMessage());
            logger.error("Database error processing command: {}", command, e);
            return true;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("Error processing command: {}", command, e);
            return true;
        }
    }

    /**
     * Prints help information.
     */
    private void printHelp() {
        System.out.println(ApplicationConfig.HELP_MESSAGE);
    }
}