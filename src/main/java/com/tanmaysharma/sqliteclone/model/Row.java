package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Represents a row in the database table.
 * Immutable data structure with proper validation and comparison.
 */
public final class Row {
    private final int id;
    private final String username;
    private final String email;

    /**
     * Creates a new row with the specified ID, username, and email.
     *
     * @param id The row ID
     * @param username The username
     * @param email The email
     * @throws ValidationException if the parameters are invalid
     */
    public Row(int id, String username, String email) {
        validateId(id);
        validateUsername(username);
        validateEmail(email);

        this.id = id;
        this.username = StringUtils.truncate(username, DatabaseConfig.MAX_USERNAME_LENGTH);
        this.email = StringUtils.truncate(email, DatabaseConfig.MAX_EMAIL_LENGTH);
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
     * Validates the ID field.
     *
     * @param id The ID to validate
     * @throws ValidationException if invalid
     */
    private void validateId(int id) {
        if (id <= 0) {
            throw new ValidationException("ID must be positive", "id", id);
        }
    }

    /**
     * Validates the username field.
     *
     * @param username The username to validate
     * @throws ValidationException if invalid
     */
    private void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ValidationException("Username cannot be null or empty", "username", username);
        }

        if (username.length() > DatabaseConfig.MAX_USERNAME_LENGTH) {
            // Log warning but don't fail - we'll truncate
            // Could add logging here if needed
        }
    }

    /**
     * Validates the email field.
     *
     * @param email The email to validate
     * @throws ValidationException if invalid
     */
    private void validateEmail(String email) {
        if (email == null) {
            throw new ValidationException("Email cannot be null", "email", email);
        }

        if (email.length() > DatabaseConfig.MAX_EMAIL_LENGTH) {
            // Log warning but don't fail - we'll truncate
            // Could add logging here if needed
        }

        System.out.println(email);

        // Basic email validation (could be enhanced)
        if (!email.isEmpty() && !email.contains("@")) {
            throw new ValidationException("Email must contain @ symbol", "email", email);
        }
    }

    /**
     * Creates a copy of this row with a new ID.
     *
     * @param newId The new ID
     * @return A new row with the updated ID
     */
    public Row withId(int newId) {
        return new Row(newId, username, email);
    }

    /**
     * Creates a copy of this row with a new username.
     *
     * @param newUsername The new username
     * @return A new row with the updated username
     */
    public Row withUsername(String newUsername) {
        return new Row(id, newUsername, email);
    }

    /**
     * Creates a copy of this row with a new email.
     *
     * @param newEmail The new email
     * @return A new row with the updated email
     */
    public Row withEmail(String newEmail) {
        return new Row(id, username, newEmail);
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
         * @throws ValidationException if the parameters are invalid
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

    /**
     * Returns a formatted string representation suitable for display.
     *
     * @return Formatted row string
     */
    public String toDisplayString() {
        return String.format("(%d, %s, %s)", id, username, email);
    }
}