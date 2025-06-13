package com.tanmaysharma.sqliteclone.model;

import com.tanmaysharma.sqliteclone.exception.ValidationException;
import com.tanmaysharma.sqliteclone.util.ValidationUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an index for fast row lookups.
 * Thread-safe implementation using concurrent data structures.
 */
public class Index {
    private final String name;
    private final String columnName;
    private final Map<Object, Integer> keyToRowIndex;
    private final boolean unique;
    private final long createdAt;

    /**
     * Creates a new index.
     *
     * @param name The index name
     * @param columnName The column being indexed
     * @param unique Whether the index enforces uniqueness
     * @throws ValidationException if parameters are invalid
     */
    public Index(String name, String columnName, boolean unique) {
        ValidationUtil.requireNonEmpty(name, "index name");
        ValidationUtil.requireNonEmpty(columnName, "column name");

        this.name = name.trim();
        this.columnName = columnName.trim();
        this.unique = unique;
        this.keyToRowIndex = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Gets the index name.
     *
     * @return The index name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the column name being indexed.
     *
     * @return The column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Checks if this is a unique index.
     *
     * @return True if unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return The creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the number of entries in the index.
     *
     * @return The entry count
     */
    public int size() {
        return keyToRowIndex.size();
    }

    /**
     * Checks if the index is empty.
     *
     * @return True if empty
     */
    public boolean isEmpty() {
        return keyToRowIndex.isEmpty();
    }

    /**
     * Adds an entry to the index.
     *
     * @param key The key value
     * @param rowIndex The row index
     * @throws ValidationException if the key already exists in a unique index
     */
    public void put(Object key, int rowIndex) {
        if (key == null) {
            throw new ValidationException("Index key cannot be null", "key", key);
        }

        ValidationUtil.requireNonNegative(rowIndex, "rowIndex");

        if (unique && keyToRowIndex.containsKey(key)) {
            throw new ValidationException(
                    "Duplicate key in unique index: " + key,
                    "key", key
            );
        }

        keyToRowIndex.put(key, rowIndex);
    }

    /**
     * Looks up a row index by key.
     *
     * @param key The key to look up
     * @return The row index, or null if not found
     */
    public Integer get(Object key) {
        if (key == null) {
            return null;
        }
        return keyToRowIndex.get(key);
    }

    /**
     * Checks if a key exists in the index.
     *
     * @param key The key to check
     * @return True if the key exists
     */
    public boolean containsKey(Object key) {
        return key != null && keyToRowIndex.containsKey(key);
    }

    /**
     * Removes an entry from the index.
     *
     * @param key The key to remove
     * @return The row index that was removed, or null if not found
     */
    public Integer remove(Object key) {
        if (key == null) {
            return null;
        }
        return keyToRowIndex.remove(key);
    }

    /**
     * Clears all entries from the index.
     */
    public void clear() {
        keyToRowIndex.clear();
    }

    /**
     * Gets all keys in the index.
     *
     * @return An array of all keys
     */
    public Object[] getKeys() {
        return keyToRowIndex.keySet().toArray();
    }

    /**
     * Creates a builder for constructing indexes.
     *
     * @return A new index builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for creating indexes.
     */
    public static class Builder {
        private String name;
        private String columnName;
        private boolean unique = false;

        /**
         * Sets the index name.
         *
         * @param name The index name
         * @return This builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the column name.
         *
         * @param columnName The column name
         * @return This builder for chaining
         */
        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        /**
         * Sets whether the index is unique.
         *
         * @param unique True for unique index
         * @return This builder for chaining
         */
        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        /**
         * Builds a new index with the set parameters.
         *
         * @return The constructed index
         * @throws ValidationException if the parameters are invalid
         */
        public Index build() {
            return new Index(name, columnName, unique);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return unique == index.unique &&
                createdAt == index.createdAt &&
                Objects.equals(name, index.name) &&
                Objects.equals(columnName, index.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnName, unique, createdAt);
    }

    @Override
    public String toString() {
        return String.format(
                "Index{name='%s', columnName='%s', unique=%s, size=%d, createdAt=%d}",
                name, columnName, unique, size(), createdAt
        );
    }
}