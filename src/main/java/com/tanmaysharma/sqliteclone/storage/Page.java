package com.tanmaysharma.sqliteclone.storage;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.exception.StorageException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a page in the database file.
 * Thread-safe implementation with access tracking and utility methods.
 */
public class Page {
    private final byte[] data;
    private final AtomicLong lastAccessed;
    private final int pageNumber;

    /**
     * Creates a new page with the specified data and page number.
     *
     * @param data The page data
     * @param pageNumber The page number
     * @throws StorageException if the data is invalid
     */
    public Page(byte[] data, int pageNumber) {
        if (data == null) {
            throw new StorageException("Page data cannot be null");
        }

        if (data.length != DatabaseConfig.PAGE_SIZE) {
            throw new StorageException(String.format(
                    "Page data must be exactly %d bytes, got %d",
                    DatabaseConfig.PAGE_SIZE, data.length
            ));
        }

        if (pageNumber < 0) {
            throw new StorageException("Page number cannot be negative");
        }

        this.data = data.clone(); // Defensive copy
        this.lastAccessed = new AtomicLong(System.currentTimeMillis());
        this.pageNumber = pageNumber;
    }

    /**
     * Creates a new empty page.
     *
     * @param pageNumber The page number
     */
    public Page(int pageNumber) {
        this(new byte[DatabaseConfig.PAGE_SIZE], pageNumber);
    }

    /**
     * Gets the page data.
     * Updates the last accessed timestamp.
     *
     * @return A copy of the page data
     */
    public byte[] getData() {
        touch();
        return data.clone(); // Return defensive copy
    }

    /**
     * Gets the raw page data without copying.
     * Use with caution - modifications will affect the original data.
     *
     * @return The raw page data
     */
    public byte[] getRawData() {
        touch();
        return data;
    }

    /**
     * Gets the page number.
     *
     * @return The page number
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Gets the time the page was last accessed.
     *
     * @return The last access time in milliseconds
     */
    public long getLastAccessed() {
        return lastAccessed.get();
    }

    /**
     * Updates the last accessed timestamp.
     */
    public void touch() {
        lastAccessed.set(System.currentTimeMillis());
    }

    /**
     * Creates a copy of this page.
     *
     * @return A new page with the same data
     */
    public Page copy() {
        return new Page(data.clone(), pageNumber);
    }

    /**
     * Clears the page data (fills with zeros).
     */
    public void clear() {
        Arrays.fill(data, (byte) 0);
        touch();
    }

    /**
     * Reads an integer from the page at the specified offset.
     *
     * @param offset The offset to read from
     * @return The integer value
     * @throws StorageException if the offset is invalid
     */
    public int readInt(int offset) {
        validateOffset(offset, Integer.BYTES);

        touch();
        return ((data[offset] & 0xFF)) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    /**
     * Writes an integer to the page at the specified offset.
     *
     * @param value The integer value to write
     * @param offset The offset to write to
     * @throws StorageException if the offset is invalid
     */
    public void writeInt(int value, int offset) {
        validateOffset(offset, Integer.BYTES);

        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);

        touch();
    }

    /**
     * Reads a byte array from the page.
     *
     * @param offset The offset to read from
     * @param length The number of bytes to read
     * @return The byte array
     * @throws StorageException if the offset or length is invalid
     */
    public byte[] readBytes(int offset, int length) {
        validateOffset(offset, length);

        touch();
        return Arrays.copyOfRange(data, offset, offset + length);
    }

    /**
     * Writes a byte array to the page.
     *
     * @param bytes The bytes to write
     * @param offset The offset to write to
     * @throws StorageException if the offset is invalid or bytes don't fit
     */
    public void writeBytes(byte[] bytes, int offset) {
        if (bytes == null) {
            throw new StorageException("Bytes cannot be null");
        }

        validateOffset(offset, bytes.length);

        System.arraycopy(bytes, 0, data, offset, bytes.length);
        touch();
    }

    /**
     * Reads a string from the page at the specified offset and length.
     *
     * @param offset The offset to read from
     * @param maxLength The maximum length to read
     * @return The string value
     * @throws StorageException if the offset or length is invalid
     */
    public String readString(int offset, int maxLength) {
        validateOffset(offset, maxLength);

        // Find null terminator
        int length = 0;
        while (length < maxLength && offset + length < data.length && data[offset + length] != 0) {
            length++;
        }

        touch();
        return new String(data, offset, length).trim();
    }

    /**
     * Writes a string to the page at the specified offset.
     *
     * @param value The string value to write
     * @param offset The offset to write to
     * @param maxLength The maximum length to write
     * @throws StorageException if the offset or length is invalid
     */
    public void writeString(String value, int offset, int maxLength) {
        validateOffset(offset, maxLength);

        // Clear the space first
        Arrays.fill(data, offset, offset + maxLength, (byte) 0);

        if (value != null && !value.isEmpty()) {
            byte[] bytes = value.getBytes();
            int bytesToWrite = Math.min(bytes.length, maxLength - 1); // Leave room for null terminator
            System.arraycopy(bytes, 0, data, offset, bytesToWrite);
        }

        touch();
    }

    /**
     * Validates that an offset and length are within page bounds.
     *
     * @param offset The offset
     * @param length The length
     * @throws StorageException if invalid
     */
    private void validateOffset(int offset, int length) {
        if (offset < 0) {
            throw new StorageException("Offset cannot be negative: " + offset);
        }

        if (length < 0) {
            throw new StorageException("Length cannot be negative: " + length);
        }

        if (offset + length > data.length) {
            throw new StorageException(String.format(
                    "Offset %d + length %d exceeds page size %d",
                    offset, length, data.length
            ));
        }
    }

    /**
     * Gets the age of this page in milliseconds.
     *
     * @return The age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - lastAccessed.get();
    }

    /**
     * Checks if the page is older than the specified age.
     *
     * @param maxAge The maximum age in milliseconds
     * @return True if the page is older than maxAge
     */
    public boolean isOlderThan(long maxAge) {
        return getAge() > maxAge;
    }

    @Override
    public String toString() {
        return String.format("Page{number=%d, lastAccessed=%d, age=%dms}",
                pageNumber, lastAccessed.get(), getAge());
    }
}