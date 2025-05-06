package com.tanmaysharma.sqliteclone;

import java.util.Objects;

/**
 * Represents a row in the database table.
 * Immutable data structure with proper validation and comparison.
 */
public class Row {
    private final int id;
    private final String username;
    private final String email;
    
    public static final int MAX_USERNAME_LENGTH = 32;
    public static final int MAX_EMAIL_LENGTH = 256;

    /**
     * Creates a new row with the specified ID, username, and email.
     *
     * @param id The row ID
     * @param username The username
     * @param email The email
     * @throws IllegalArgumentException if the parameters are invalid
     */
    public Row(int id, String username, String email) {
        // Validate ID
        if (id < 0) {
            throw new IllegalArgumentException("ID cannot be negative");
        }
        
        // Validate username
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        
        // Validate email
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        
        this.id = id;
        this.username = username;
        this.email = email;
    }

    /**
     * Gets the row ID.
     *
     * @return The row ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the email.
     *
     * @return The email
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Creates a builder for constructing rows.
     *
     * @return A new row builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder pattern for creating rows with validation.
     */
    public static class Builder {
        private int id;
        private String username;
        private String email;
        
        /**
         * Sets the row ID.
         *
         * @param id The row ID
         * @return This builder for chaining
         */
        public Builder id(int id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the username.
         *
         * @param username The username
         * @return This builder for chaining
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        /**
         * Sets the email.
         *
         * @param email The email
         * @return This builder for chaining
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        /**
         * Builds a new row with the set parameters.
         *
         * @return The constructed row
         * @throws IllegalArgumentException if the parameters are invalid
         */
        public Row build() {
            return new Row(id, username, email);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return id == row.id &&
               Objects.equals(username, row.username) &&
               Objects.equals(email, row.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }
    
    @Override
    public String toString() {
        return String.format("Row{id=%d, username='%s', email='%s'}", id, username, email);
    }
}