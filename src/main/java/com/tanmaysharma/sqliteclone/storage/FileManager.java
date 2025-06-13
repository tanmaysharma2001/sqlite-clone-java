package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.exception.StorageException;
import com.tanmaysharma.sqliteclone.util.ValidationUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages database file operations including backup, restore, and maintenance.
 * Provides file system utilities for database management.
 */
public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String databaseFilename;
    private final Path databasePath;
    private final Path backupDirectory;

    /**
     * Creates a new file manager for the specified database file.
     *
     * @param databaseFilename The database filename
     * @throws StorageException if the file path is invalid
     */
    public FileManager(String databaseFilename) throws StorageException {
        ValidationUtil.requireNonEmpty(databaseFilename, "databaseFilename");

        this.databaseFilename = databaseFilename;
        this.databasePath = Paths.get(databaseFilename);

        // Create backup directory in the same location as the database
        Path parentDir = databasePath.getParent();
        if (parentDir == null) {
            parentDir = Paths.get(".");
        }
        this.backupDirectory = parentDir.resolve("backups");

        try {
            // Ensure backup directory exists
            Files.createDirectories(backupDirectory);
            logger.info("File manager initialized for database: {}", databaseFilename);
        } catch (IOException e) {
            throw new StorageException("Failed to create backup directory", e);
        }
    }

    /**
     * Checks if the database file exists.
     *
     * @return True if the file exists
     */
    public boolean databaseExists() {
        return Files.exists(databasePath);
    }

    /**
     * Gets the size of the database file in bytes.
     *
     * @return The file size, or 0 if the file doesn't exist
     */
    public long getDatabaseSize() {
        try {
            return Files.exists(databasePath) ? Files.size(databasePath) : 0;
        } catch (IOException e) {
            logger.warn("Failed to get database file size", e);
            return 0;
        }
    }

    /**
     * Gets the last modified time of the database file.
     *
     * @return The last modified time in milliseconds, or 0 if the file doesn't exist
     */
    public long getLastModified() {
        try {
            return Files.exists(databasePath) ?
                    Files.getLastModifiedTime(databasePath).toMillis() : 0;
        } catch (IOException e) {
            logger.warn("Failed to get database file modification time", e);
            return 0;
        }
    }

    /**
     * Creates a backup of the database file.
     *
     * @return The path to the created backup file
     * @throws StorageException if the backup fails
     */
    public Path createBackup() throws StorageException {
        if (!databaseExists()) {
            throw new StorageException("Database file does not exist: " + databaseFilename);
        }

        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        String backupFilename = String.format("%s_backup_%s.db",
                getFileNameWithoutExtension(databaseFilename), timestamp);

        Path backupPath = backupDirectory.resolve(backupFilename);

        try {
            Files.copy(databasePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Database backup created: {}", backupPath);
            return backupPath;
        } catch (IOException e) {
            throw new StorageException("Failed to create backup", e);
        }
    }

    /**
     * Restores the database from a backup file.
     *
     * @param backupPath The path to the backup file
     * @throws StorageException if the restore fails
     */
    public void restoreFromBackup(Path backupPath) throws StorageException {
        ValidationUtil.requireNonNull(backupPath, "backupPath");

        if (!Files.exists(backupPath)) {
            throw new StorageException("Backup file does not exist: " + backupPath);
        }

        try {
            // Create a backup of current file before restoring
            if (databaseExists()) {
                String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
                String preRestoreBackup = String.format("%s_pre_restore_%s.db",
                        getFileNameWithoutExtension(databaseFilename), timestamp);
                Path preRestorePath = backupDirectory.resolve(preRestoreBackup);
                Files.copy(databasePath, preRestorePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Pre-restore backup created: {}", preRestorePath);
            }

            // Restore from backup
            Files.copy(backupPath, databasePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Database restored from backup: {}", backupPath);

        } catch (IOException e) {
            throw new StorageException("Failed to restore from backup", e);
        }
    }

    /**
     * Lists all available backup files.
     *
     * @return An array of backup file paths
     * @throws StorageException if listing fails
     */
    public Path[] listBackups() throws StorageException {
        try {
            if (!Files.exists(backupDirectory)) {
                return new Path[0];
            }

            return Files.list(backupDirectory)
                    .filter(path -> path.toString().endsWith(".db"))
                    .filter(path -> path.getFileName().toString().contains("backup"))
                    .sorted((p1, p2) -> {
                        try {
                            // Sort by modification time, newest first
                            return Files.getLastModifiedTime(p2)
                                    .compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return p1.compareTo(p2);
                        }
                    })
                    .toArray(Path[]::new);

        } catch (IOException e) {
            throw new StorageException("Failed to list backup files", e);
        }
    }

    /**
     * Deletes old backup files, keeping only the specified number of recent backups.
     *
     * @param keepCount The number of backups to keep
     * @return The number of backups deleted
     * @throws StorageException if cleanup fails
     */
    public int cleanupOldBackups(int keepCount) throws StorageException {
        if (keepCount < 0) {
            throw new IllegalArgumentException("Keep count cannot be negative");
        }

        Path[] backups = listBackups();

        if (backups.length <= keepCount) {
            return 0; // Nothing to delete
        }

        int deletedCount = 0;

        // Delete old backups (array is already sorted by modification time)
        for (int i = keepCount; i < backups.length; i++) {
            try {
                Files.delete(backups[i]);
                deletedCount++;
                logger.debug("Deleted old backup: {}", backups[i]);
            } catch (IOException e) {
                logger.warn("Failed to delete backup file: {}", backups[i], e);
            }
        }

        logger.info("Cleaned up {} old backup files", deletedCount);
        return deletedCount;
    }

    /**
     * Compacts the database file by removing unused space.
     * This is a placeholder for future implementation.
     *
     * @throws StorageException if compaction fails
     */
    public void compactDatabase() throws StorageException {
        if (!databaseExists()) {
            throw new StorageException("Database file does not exist");
        }

        // TODO: Implement database compaction
        // This would involve:
        // 1. Reading all valid data
        // 2. Writing to a temporary file
        // 3. Replacing the original file
        // 4. Updating any indexes

        logger.info("Database compaction not yet implemented");
    }

    /**
     * Validates the integrity of the database file.
     * This is a basic check - a full implementation would verify data structures.
     *
     * @return True if the file appears to be valid
     */
    public boolean validateDatabaseIntegrity() {
        if (!databaseExists()) {
            return false;
        }

        try {
            // Basic validation - check if file is readable and has reasonable size
            long size = getDatabaseSize();

            if (size < 0) {
                return false;
            }

            // Check if file is readable
            return Files.isReadable(databasePath) && Files.isWritable(databasePath);

        } catch (Exception e) {
            logger.warn("Database integrity check failed", e);
            return false;
        }
    }

    /**
     * Gets database file information.
     *
     * @return A formatted string with file information
     */
    public String getDatabaseInfo() {
        if (!databaseExists()) {
            return "Database file does not exist";
        }

        try {
            long size = getDatabaseSize();
            long lastModified = getLastModified();
            String lastModifiedStr = lastModified > 0 ?
                    LocalDateTime.ofEpochSecond(lastModified / 1000, 0,
                                    java.time.ZoneOffset.systemDefault().getRules()
                                            .getOffset(java.time.Instant.ofEpochMilli(lastModified)))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "Unknown";

            return String.format(
                    "Database File Information:\n" +
                            "  Path: %s\n" +
                            "  Size: %s\n" +
                            "  Last Modified: %s\n" +
                            "  Readable: %s\n" +
                            "  Writable: %s",
                    databasePath.toAbsolutePath(),
                    FileUtils.byteCountToDisplaySize(size),
                    lastModifiedStr,
                    Files.isReadable(databasePath),
                    Files.isWritable(databasePath)
            );

        } catch (Exception e) {
            return "Error retrieving database information: " + e.getMessage();
        }
    }

    /**
     * Gets the filename without extension.
     *
     * @param filename The full filename
     * @return The filename without extension
     */
    private String getFileNameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    /**
     * Gets the backup directory path.
     *
     * @return The backup directory path
     */
    public Path getBackupDirectory() {
        return backupDirectory;
    }

    /**
     * Gets the database file path.
     *
     * @return The database file path
     */
    public Path getDatabasePath() {
        return databasePath;
    }
}