package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Table Tests")
class TableTest {

    @Nested
    @DisplayName("Table Creation")
    class TableCreation {

        @Test
        @DisplayName("Should create table with default configuration")
        void shouldCreateTableWithDefaultConfiguration() {
            Table table = new Table("test_table");

            assertEquals("test_table", table.getName());
            assertEquals(DatabaseConfig.TABLE_MAX_ROWS, table.getMaxRows());
            assertEquals(DatabaseConfig.ROW_SIZE, table.getRowSize());
            assertEquals(DatabaseConfig.PAGE_SIZE, table.getPageSize());
            assertTrue(table.getCreatedAt() > 0);
        }

        @Test
        @DisplayName("Should create table with custom configuration")
        void shouldCreateTableWithCustomConfiguration() {
            long createdAt = System.currentTimeMillis();
            Table table = new Table("custom_table", 500, 200, 4096, createdAt);

            assertEquals("custom_table", table.getName());
            assertEquals(500, table.getMaxRows());
            assertEquals(200, table.getRowSize());
            assertEquals(4096, table.getPageSize());
            assertEquals(createdAt, table.getCreatedAt());
        }

        @Test
        @DisplayName("Should reject null table name")
        void shouldRejectNullTableName() {
            assertThrows(ValidationException.class, () -> new Table(null));
        }

        @Test
        @DisplayName("Should reject empty table name")
        void shouldRejectEmptyTableName() {
            assertThrows(ValidationException.class, () -> new Table(""));
        }

        @Test
        @DisplayName("Should reject negative max rows")
        void shouldRejectNegativeMaxRows() {
            assertThrows(ValidationException.class, () ->
                    new Table("test", -1, 200, 4096, System.currentTimeMillis()));
        }

        @Test
        @DisplayName("Should reject page size smaller than row size")
        void shouldRejectPageSizeSmallerThanRowSize() {
            assertThrows(ValidationException.class, () ->
                    new Table("test", 100, 4096, 200, System.currentTimeMillis()));
        }
    }

    @Nested
    @DisplayName("Table Calculations")
    class TableCalculations {

        @Test
        @DisplayName("Should calculate rows per page correctly")
        void shouldCalculateRowsPerPageCorrectly() {
            Table table = new Table("test", 1000, 100, 1000, System.currentTimeMillis());
            assertEquals(10, table.getRowsPerPage());
        }

        @Test
        @DisplayName("Should calculate max pages correctly")
        void shouldCalculateMaxPagesCorrectly() {
            Table table = new Table("test", 150, 100, 1000, System.currentTimeMillis());
            // 150 rows / 10 rows per page = 15 pages
            assertEquals(15, table.getMaxPages());
        }

        @Test
        @DisplayName("Should calculate page for row correctly")
        void shouldCalculatePageForRowCorrectly() {
            Table table = new Table("test", 1000, 100, 1000, System.currentTimeMillis());

            assertEquals(0, table.getPageForRow(5));  // Row 5 is on page 0
            assertEquals(1, table.getPageForRow(15)); // Row 15 is on page 1
            assertEquals(2, table.getPageForRow(25)); // Row 25 is on page 2
        }

        @Test
        @DisplayName("Should calculate row offset in page correctly")
        void shouldCalculateRowOffsetInPageCorrectly() {
            Table table = new Table("test", 1000, 100, 1000, System.currentTimeMillis());

            assertEquals(5, table.getRowOffsetInPage(5));  // Row 5 offset = 5
            assertEquals(5, table.getRowOffsetInPage(15)); // Row 15 offset = 5
            assertEquals(5, table.getRowOffsetInPage(25)); // Row 25 offset = 5
        }

        @Test
        @DisplayName("Should throw exception for invalid row index")
        void shouldThrowExceptionForInvalidRowIndex() {
            Table table = new Table("test", 100, 100, 1000, System.currentTimeMillis());

            assertThrows(IllegalArgumentException.class, () -> table.getPageForRow(-1));
            assertThrows(IllegalArgumentException.class, () -> table.getPageForRow(100));
        }
    }

    @Nested
    @DisplayName("Table Builder")
    class TableBuilder {

        @Test
        @DisplayName("Should build table with builder pattern")
        void shouldBuildTableWithBuilderPattern() {
            Table table = Table.builder()
                    .name("builder_table")
                    .maxRows(500)
                    .rowSize(150)
                    .pageSize(3000)
                    .build();

            assertEquals("builder_table", table.getName());
            assertEquals(500, table.getMaxRows());
            assertEquals(150, table.getRowSize());
            assertEquals(3000, table.getPageSize());
        }

        @Test
        @DisplayName("Should validate during build")
        void shouldValidateDuringBuild() {
            assertThrows(ValidationException.class, () ->
                    Table.builder()
                            .name("")  // Empty name should fail
                            .build());
        }
    }

    @Test
    @DisplayName("Should have proper equals and hashCode")
    void shouldHaveProperEqualsAndHashCode() {
        long createdAt = System.currentTimeMillis();
        Table table1 = new Table("test", 100, 200, 4000, createdAt);
        Table table2 = new Table("test", 100, 200, 4000, createdAt);
        Table table3 = new Table("different", 100, 200, 4000, createdAt);

        assertEquals(table1, table2);
        assertNotEquals(table1, table3);
        assertEquals(table1.hashCode(), table2.hashCode());
    }
}