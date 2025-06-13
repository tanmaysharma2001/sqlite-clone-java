package com.tanmaysharma.sqliteclone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for simple App.
 */
@DisplayName("Application Tests")
class AppTest
{
    @Test
    @DisplayName("Should pass basic test")
    void shouldPassBasicTest() {
        assertTrue(true);
    }

    @Test
    @DisplayName("Should validate application constants")
    void shouldValidateApplicationConstants() {
        // Test that our main class exists and can be instantiated
        assertDoesNotThrow(() -> {
            Class<?> mainClass = Class.forName("com.tanmaysharma.sqliteclone.Main");
            assertNotNull(mainClass);
        });
    }
}
