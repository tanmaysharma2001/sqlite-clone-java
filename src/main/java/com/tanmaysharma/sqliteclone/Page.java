package com.tanmaysharma.sqliteclone;

import java.util.Arrays;

/**
 * Represents a page in the database file.
 * Manages page data and provides utility methods for data access.
 */
public class Page {
    private final byte[] data;
    private long lastAccessed;

    /**
     * Creates a new page with the specified data.
     *
     * @param data The page data
     * @throws IllegalArgumentException if the data is null or has invalid length
     */
    public Page(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Page data cannot be null");
        }
        
        if (data.length != Database.PAGE_SIZE) {
            throw new IllegalArgumentException("Page data must be exactly " + 
                                              Database.PAGE_SIZE + " bytes");
        }
        
        this.data = data;
        this.lastAccessed = System.currentTimeMillis();
    }

    /**
     * Gets the page data.
     *
     * @return The page data
     */
    public byte[] getData() {
        // Update access time
        this.lastAccessed = System.currentTimeMillis();
        return data;
    }
    
    /**
     * Gets the time the page was last accessed.
     *
     * @return The last access time in milliseconds
     */
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    /**
     * Updates the last accessed timestamp.
     */
    public void touch() {
        this.lastAccessed = System.currentTimeMillis();
    }
    
    /**
     * Creates a copy of this page.
     *
     * @return A new page with the same data
     */
    public Page copy() {
        return new Page(Arrays.copyOf(data, data.length));
    }
    
    /**
     * Clears the page data.
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
     * @throws IndexOutOfBoundsException if the offset is invalid
     */
    public int readInt(int offset) {
        if (offset < 0 || offset + 3 >= data.length) {
            throw new IndexOutOfBoundsException("Invalid offset: " + offset);
        }
        
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
     * @throws IndexOutOfBoundsException if the offset is invalid
     */
    public void writeInt(int value, int offset) {
        if (offset < 0 || offset + 3 >= data.length) {
            throw new IndexOutOfBoundsException("Invalid offset: " + offset);
        }
        
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
        
        touch();
    }
    
    /**
     * Reads a string from the page at the specified offset and length.
     *
     * @param offset The offset to read from
     * @param length The maximum length to read
     * @return The string value
     * @throws IndexOutOfBoundsException if the offset or length is invalid
     */
    public String readString(int offset, int length) {
        if (offset < 0 || offset >= data.length || length < 0) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        
        int end = Math.min(offset + length, data.length);
        
        // Find null terminator
        for (int i = offset; i < end; i++) {
            if (data[i] == 0) {
                end = i;
                break;
            }
        }
        
        touch();
        return new String(data, offset, end - offset);
    }
    
    /**
     * Writes a string to the page at the specified offset.
     *
     * @param value The string value to write
     * @param offset The offset to write to
     * @param maxLength The maximum length to write
     * @throws IndexOutOfBoundsException if the offset or length is invalid
     */
    public void writeString(String value, int offset, int maxLength) {
        if (offset < 0 || offset >= data.length || maxLength <= 0) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        
        int end = Math.min(offset + maxLength, data.length);
        
        // Clear the space first
        Arrays.fill(data, offset, end, (byte) 0);
        
        if (value != null && !value.isEmpty()) {
            byte[] bytes = value.getBytes();
            int bytesToWrite = Math.min(bytes.length, maxLength);
            System.arraycopy(bytes, 0, data, offset, bytesToWrite);
        }
        
        touch();
    }
}