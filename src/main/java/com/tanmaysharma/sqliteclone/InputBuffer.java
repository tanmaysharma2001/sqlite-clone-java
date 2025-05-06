package com.tanmaysharma.sqliteclone;

import java.util.Scanner;

/**
 * Manages user input for the database REPL.
 * Handles input reading and processing.
 */
public class InputBuffer {
    private String buffer;
    private int inputLength;
    private static final int MAX_INPUT_LENGTH = 1024;

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
     * @throws IllegalArgumentException if the input is too long
     */
    public void readInput(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        
        String input = scanner.nextLine();
        
        // Check input length
        if (input != null && input.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Input exceeds maximum length of " + MAX_INPUT_LENGTH + " characters");
        }
        
        this.buffer = input != null ? input.trim() : "";
        this.inputLength = this.buffer.length();
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
        return this.buffer.isEmpty();
    }
}