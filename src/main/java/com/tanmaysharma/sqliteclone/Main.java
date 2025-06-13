package com.tanmaysharma.sqliteclone;

import com.tanmaysharma.sqliteclone.cli.CommandLineInterface;
import com.tanmaysharma.sqliteclone.config.ApplicationConfig;
import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.DatabaseException;
import com.tanmaysharma.sqliteclone.query.QueryEngine;
import com.tanmaysharma.sqliteclone.query.QueryEngineImpl;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import com.tanmaysharma.sqliteclone.storage.StorageEngineImpl;
import com.tanmaysharma.sqliteclone.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main application class for the SQLite Clone.
 * Coordinates all components and manages application lifecycle.
 *
 * Usage:
 * mvn exec:java -Dexec.args="test.db"
 * or
 * java -cp target/sqlite-clone-java-2.0.0.jar com.tanmaysharma.sqliteclone.Main test.db
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;
    private final CommandLineInterface cli;
    private final String databaseFilename;

    /**
     * Creates a new application instance.
     *
     * @param databaseFilename The database file to use
     */
    public Main(String databaseFilename) {
        this.databaseFilename = databaseFilename;

        // Initialize components with dependency injection
        this.storageEngine = new StorageEngineImpl();
        this.queryEngine = new QueryEngineImpl(storageEngine);
        this.cli = new CommandLineInterface(queryEngine, databaseFilename);

        logger.info("Application initialized for database: {}", databaseFilename);
    }

    /**
     * Starts the application.
     */
    public void start() {
        try {
            logger.info("Starting SQLite Clone application");

            // Validate configuration before starting
            DatabaseConfig.validate();

            // Initialize storage
            storageEngine.initialize(databaseFilename);

            // Display welcome message
            System.out.println(ApplicationConfig.getWelcomeMessage(databaseFilename));

            // Start the CLI
            cli.start();

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.err.println("Fatal error: " + e.getMessage());
            shutdown();
            System.exit(1);
        }
    }

    /**
     * Shuts down the application gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down application");

        try {
            // Close CLI
            if (cli != null) {
                cli.shutdown();
            }

            // Close storage engine
            if (storageEngine != null) {
                storageEngine.close();
            }

            logger.info("Application shutdown complete");

        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments (database filename)
     */
    public static void main(String[] args) {
        // Set up logging
        LoggerUtil.configureLogging();

        // Validate arguments
        if (args.length < 1) {
            System.out.println("Usage: java -jar sqlite-clone-java.jar <database_file>");
            System.out.println("Example: java -jar sqlite-clone-java.jar test.db");
            System.exit(1);
        }

        String databaseFilename = args[0];

        try {
            // Validate database filename
            validateDatabaseFilename(databaseFilename);

            // Create and start application
            Main app = new Main(databaseFilename);

            // Add shutdown hook for graceful cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down database...");
                app.shutdown();
            }));

            // Start the application
            app.start();

        } catch (Exception e) {
            logger.error("Fatal error in main", e);
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Validates the database filename.
     *
     * @param filename The filename to validate
     * @throws DatabaseException if the filename is invalid
     */
    private static void validateDatabaseFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new DatabaseException("Database filename cannot be empty");
        }

        File file = new File(filename);
        File parentDir = file.getParentFile();

        // Ensure parent directory exists or can be created
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new DatabaseException("Cannot create directory: " + parentDir);
            }
        }

        // Check if file can be created or is writable
        if (file.exists() && !file.canWrite()) {
            throw new DatabaseException("Cannot write to file: " + filename);
        }

        logger.debug("Database filename validated: {}", filename);
    }
}