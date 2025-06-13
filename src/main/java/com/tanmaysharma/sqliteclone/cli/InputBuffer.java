package com.tanmaysharma.sqliteclone.cli;

import com.tanmaysharma.sqliteclone.config.ApplicationConfig;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

/**
 * Manages user input for the database REPL.
 * Handles input reading, validation, and processing.
 */
public class InputBuffer {
    private String buffer;
    private int inputLength;

    /**
     * Creates a new input buffer.
     */
    public InputBuffer() {
        this.buffer = "";
        this.inputLength = 0;
    }

    /**
     * Reads input from the provided scanner.
     *
     * @param scanner The scanner to read from
     * @throws ValidationException if the input is invalid
     */
    public void readInput(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }

        String input = scanner.nextLine();

        // Handle null input
        if (input == null) {
            this.buffer = "";
            this.inputLength = 0;
            return;
        }

        // Trim the input
        input = input.trim();

        // Check input length
        if (input.length() > ApplicationConfig.MAX_INPUT_LENGTH) {
            throw new ValidationException(
                    "Input exceeds maximum length of " + ApplicationConfig.MAX_INPUT_LENGTH + " characters",
                    "input", input
            );
        }

        this.buffer = input;
        this.inputLength = input.length();
    }

    /**
     * Gets the current buffer contents.
     *
     * @return The buffer contents
     */
    public String getBuffer() {
        return this.buffer;
    }

    /**
     * Gets the length of the current input.
     *
     * @return The input length
     */
    public int getInputLength() {
        return this.inputLength;
    }

    /**
     * Clears the buffer.
     */
    public void clear() {
        this.buffer = "";
        this.inputLength = 0;
    }

    /**
     * Checks if the buffer is empty.
     *
     * @return True if the buffer is empty, false otherwise
     */
    public boolean isEmpty() {
        return StringUtils.isBlank(this.buffer);
    }

    /**
     * Checks if the buffer contains a meta command.
     *
     * @return True if the buffer starts with a dot
     */
    public boolean isMetaCommand() {
        return !isEmpty() && buffer.startsWith(".");
    }

    /**
     * Gets a trimmed version of the buffer.
     *
     * @return The trimmed buffer contents
     */
    public String getTrimmedBuffer() {
        return buffer.trim();
    }

    @Override
    public String toString() {
        return String.format("InputBuffer{buffer='%s', length=%d}", buffer, inputLength);
    }
}