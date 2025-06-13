package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Row Tests")
class RowTest {

    @Nested
    @DisplayName("Row Creation")
    class RowCreation {

        @Test
        @DisplayName("Should create valid row")
        void shouldCreateValidRow() {
            Row row = new Row(1, "testuser", "test@example.com");

            assertEquals(1, row.getId());
            assertEquals("testuser", row.getUsername());
            assertEquals("test@example.com", row.getEmail());
        }

        @Test
        @DisplayName("Should reject negative ID")
        void shouldRejectNegativeId() {
            assertThrows(ValidationException.class, () ->
                    new Row(-1, "testuser", "test@example.com"));
        }

        @Test
        @DisplayName("Should reject zero ID")
        void shouldRejectZeroId() {
            assertThrows(ValidationException.class, () ->
                    new Row(0, "testuser", "test@example.com"));
        }

        @Test
        @DisplayName("Should reject null username")
        void shouldRejectNullUsername() {
            assertThrows(ValidationException.class, () ->
                    new Row(1, null, "test@example.com"));
        }

        @Test
        @DisplayName("Should reject empty username")
        void shouldRejectEmptyUsername() {
            assertThrows(ValidationException.class, () ->
                    new Row(1, "", "test@example.com"));
        }

        @Test
        @DisplayName("Should reject null email")
        void shouldRejectNullEmail() {
            assertThrows(ValidationException.class, () ->
                    new Row(1, "testuser", null));
        }

        @Test
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmailFormat() {
            assertThrows(ValidationException.class, () ->
                    new Row(1, "testuser", "invalid-email"));
        }

        @Test
        @DisplayName("Should accept empty email")
        void shouldAcceptEmptyEmail() {
            assertDoesNotThrow(() ->
                    new Row(1, "testuser", ""));
        }
    }

    @Nested
    @DisplayName("Row Builder")
    class RowBuilder {

        @Test
        @DisplayName("Should build valid row")
        void shouldBuildValidRow() {
            Row row = Row.builder()
                    .id(1)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            assertEquals(1, row.getId());
            assertEquals("testuser", row.getUsername());
            assertEquals("test@example.com", row.getEmail());
        }

        @Test
        @DisplayName("Should validate during build")
        void shouldValidateDuringBuild() {
            assertThrows(ValidationException.class, () ->
                    Row.builder()
                            .id(-1)
                            .username("testuser")
                            .email("test@example.com")
                            .build());
        }
    }

    @Nested
    @DisplayName("Row Immutability")
    class RowImmutability {

        @Test
        @DisplayName("Should create new row with different ID")
        void shouldCreateNewRowWithDifferentId() {
            Row original = new Row(1, "testuser", "test@example.com");
            Row modified = original.withId(2);

            assertEquals(1, original.getId());
            assertEquals(2, modified.getId());
            assertEquals("testuser", modified.getUsername());
            assertEquals("test@example.com", modified.getEmail());
        }

        @Test
        @DisplayName("Should create new row with different username")
        void shouldCreateNewRowWithDifferentUsername() {
            Row original = new Row(1, "testuser", "test@example.com");
            Row modified = original.withUsername("newuser");

            assertEquals("testuser", original.getUsername());
            assertEquals("newuser", modified.getUsername());
            assertEquals(1, modified.getId());
            assertEquals("test@example.com", modified.getEmail());
        }
    }

    @Test
    @DisplayName("Should have proper equals and hashCode")
    void shouldHaveProperEqualsAndHashCode() {
        Row row1 = new Row(1, "testuser", "test@example.com");
        Row row2 = new Row(1, "testuser", "test@example.com");
        Row row3 = new Row(2, "testuser", "test@example.com");

        assertEquals(row1, row2);
        assertNotEquals(row1, row3);
        assertEquals(row1.hashCode(), row2.hashCode());
    }
}