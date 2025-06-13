package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.StorageException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Page Manager Tests")
class PageManagerTest {

    @TempDir
    Path tempDir;

    private PageManager pageManager;
    private Path testDbFile;

    @BeforeEach
    void setUp() {
        testDbFile = tempDir.resolve("test.db");
        pageManager = new PageManagerImpl(testDbFile.toString());
    }

    @AfterEach
    void tearDown() throws StorageException {
        if (pageManager != null) {
            pageManager.close();
        }
    }

    @Nested
    @DisplayName("Page Retrieval")
    class PageRetrieval {

        @Test
        @DisplayName("Should get page within bounds")
        void shouldGetPageWithinBounds() throws StorageException {
            Page page = pageManager.getPage(0);

            assertNotNull(page);
            assertEquals(0, page.getPageNumber());
            assertEquals(DatabaseConfig.PAGE_SIZE, page.getData().length);
        }

        @Test
        @DisplayName("Should get multiple different pages")
        void shouldGetMultipleDifferentPages() throws StorageException {
            Page page0 = pageManager.getPage(0);
            Page page1 = pageManager.getPage(1);

            assertNotNull(page0);
            assertNotNull(page1);
            assertEquals(0, page0.getPageNumber());
            assertEquals(1, page1.getPageNumber());
            assertNotSame(page0, page1);
        }

        @Test
        @DisplayName("Should cache pages for repeated access")
        void shouldCachePagesForRepeatedAccess() throws StorageException {
            Page page1 = pageManager.getPage(0);
            Page page2 = pageManager.getPage(0);

            // Should be the same instance due to caching
            assertSame(page1, page2);
        }

        @Test
        @DisplayName("Should reject negative page number")
        void shouldRejectNegativePageNumber() {
            assertThrows(StorageException.class, () -> pageManager.getPage(-1));
        }

        @Test
        @DisplayName("Should reject page number exceeding maximum")
        void shouldRejectPageNumberExceedingMaximum() {
            assertThrows(StorageException.class, () ->
                    pageManager.getPage(DatabaseConfig.MAX_PAGES));
        }
    }

    @Nested
    @DisplayName("Page Modification")
    class PageModification {

        @Test
        @DisplayName("Should mark page as dirty")
        void shouldMarkPageAsDirty() throws StorageException {
            // Get a page first
            pageManager.getPage(0);

            // Mark it as dirty
            pageManager.markPageDirty(0);

            assertTrue(pageManager.isPageDirty(0));
        }

        @Test
        @DisplayName("Should handle dirty status correctly")
        void shouldHandleDirtyStatusCorrectly() throws StorageException {
            // Initially page should not be dirty
            assertFalse(pageManager.isPageDirty(0));

            // Mark as dirty
            pageManager.markPageDirty(0);
            assertTrue(pageManager.isPageDirty(0));
        }

        @Test
        @DisplayName("Should flush dirty page")
        void shouldFlushDirtyPage() throws StorageException {
            // Get and modify page
            Page page = pageManager.getPage(0);
            page.writeInt(42, 0);
            pageManager.markPageDirty(0);

            assertTrue(pageManager.isPageDirty(0));

            // Flush the page
            pageManager.flushPage(0);

            // Page should no longer be dirty
            assertFalse(pageManager.isPageDirty(0));
        }

        @Test
        @DisplayName("Should flush all dirty pages")
        void shouldFlushAllDirtyPages() throws StorageException {
            // Get and modify multiple pages
            Page page0 = pageManager.getPage(0);
            Page page1 = pageManager.getPage(1);

            page0.writeInt(42, 0);
            page1.writeInt(24, 0);

            pageManager.markPageDirty(0);
            pageManager.markPageDirty(1);

            assertTrue(pageManager.isPageDirty(0));
            assertTrue(pageManager.isPageDirty(1));

            // Flush all pages
            pageManager.flushAllPages();

            // No pages should be dirty
            assertFalse(pageManager.isPageDirty(0));
            assertFalse(pageManager.isPageDirty(1));
        }
    }

    @Nested
    @DisplayName("Page Persistence")
    class PagePersistence {

        @Test
        @DisplayName("Should persist page modifications across sessions")
        void shouldPersistPageModificationsAcrossSessions() throws StorageException {
            // First session - write data
            Page page = pageManager.getPage(0);
            page.writeInt(123456, 0);
            pageManager.markPageDirty(0);
            pageManager.flushPage(0);
            pageManager.close();

            // Second session - read data
            pageManager = new PageManagerImpl(testDbFile.toString());
            Page loadedPage = pageManager.getPage(0);

            assertEquals(123456, loadedPage.readInt(0));
        }
    }

    @Nested
    @DisplayName("Cache Statistics")
    class CacheStatistics {

        @Test
        @DisplayName("Should provide cache statistics")
        void shouldProvideCacheStatistics() throws StorageException {
            // Generate some cache activity
            pageManager.getPage(0);
            pageManager.getPage(1);
            pageManager.getPage(0); // Should be a cache hit

            String stats = pageManager.getCacheStatistics();

            assertNotNull(stats);
            assertFalse(stats.isEmpty());
            assertTrue(stats.contains("Cache"));
        }
    }

    @Nested
    @DisplayName("Resource Management")
    class ResourceManagement {

        @Test
        @DisplayName("Should handle close operation gracefully")
        void shouldHandleCloseOperationGracefully() throws StorageException {
            // Get and modify a page
            Page page = pageManager.getPage(0);
            page.writeInt(42, 0);
            pageManager.markPageDirty(0);

            // Close should flush all dirty pages
            assertDoesNotThrow(() -> pageManager.close());
        }

        @Test
        @DisplayName("Should prevent operations after close")
        void shouldPreventOperationsAfterClose() throws StorageException {
            pageManager.close();

            // Operations should fail after close
            assertThrows(StorageException.class, () -> pageManager.getPage(0));
        }
    }
}