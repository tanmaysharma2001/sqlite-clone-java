package com.tanmaysharma.sqliteclone.cli;

import com.tanmaysharma.sqliteclone.enums.MetaCommandResult;
import com.tanmaysharma.sqliteclone.query.QueryEngine;
import com.tanmaysharma.sqliteclone.query.QueryEngineImpl;
import com.tanmaysharma.sqliteclone.storage.StorageEngine;
import com.tanmaysharma.sqliteclone.storage.StorageEngineImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Command Processor Tests")
class CommandProcessorTest {

    @TempDir
    Path tempDir;

    private CommandProcessor commandProcessor;
    private StorageEngine storageEngine;
    private QueryEngine queryEngine;
    private Path testDbFile;

    @BeforeEach
    void setUp() throws Exception {
        testDbFile = tempDir.resolve("test.db");
        storageEngine = new StorageEngineImpl();
        storageEngine.initialize(testDbFile.toString());
        queryEngine = new QueryEngineImpl(storageEngine);
        commandProcessor = new CommandProcessor(queryEngine, testDbFile.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (storageEngine != null) {
            storageEngine.close();
        }
    }

    @Test
    @DisplayName("Should process help meta command")
    void shouldProcessHelpMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".help");
        assertEquals(MetaCommandResult.META_COMMAND_SUCCESS, result);
    }

    @Test
    @DisplayName("Should process exit meta command")
    void shouldProcessExitMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".exit");
        assertEquals(MetaCommandResult.META_COMMAND_EXIT, result);
    }

    @Test
    @DisplayName("Should process stats meta command")
    void shouldProcessStatsMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".stats");
        assertEquals(MetaCommandResult.META_COMMAND_SUCCESS, result);
    }

    @Test
    @DisplayName("Should process info meta command")
    void shouldProcessInfoMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".info");
        assertEquals(MetaCommandResult.META_COMMAND_SUCCESS, result);
    }

    @Test
    @DisplayName("Should process clear meta command")
    void shouldProcessClearMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".clear");
        assertEquals(MetaCommandResult.META_COMMAND_SUCCESS, result);
    }

    @Test
    @DisplayName("Should handle unrecognized meta command")
    void shouldHandleUnrecognizedMetaCommand() {
        MetaCommandResult result = commandProcessor.processMetaCommand(".unknown");
        assertEquals(MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND, result);
    }

    @Test
    @DisplayName("Should process SQL insert command")
    void shouldProcessSqlInsertCommand() {
        assertDoesNotThrow(() -> {
            commandProcessor.processSqlCommand("insert 1 testuser test@example.com");
        });
    }

    @Test
    @DisplayName("Should process SQL select command")
    void shouldProcessSqlSelectCommand() {
        assertDoesNotThrow(() -> {
            commandProcessor.processSqlCommand("select");
        });
    }
}