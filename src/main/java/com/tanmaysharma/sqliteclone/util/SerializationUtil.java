package com.tanmaysharma.sqliteclone.util;

import com.tanmaysharma.sqliteclone.config.DatabaseConfig;
import com.tanmaysharma.sqliteclone.model.Row;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for row serialization and deserialization operations.
 */
public final class SerializationUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private SerializationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Serializes a row to a byte array.
     *
     * @param row The row to serialize
     * @return The serialized row data
     */
    public static byte[] serializeRow(Row row) {
        byte[] data = new byte[DatabaseConfig.ROW_SIZE];

        // Write ID (4 bytes, little-endian)
        writeInt(data, DatabaseConfig.ID_OFFSET, row.getId());

        // Write username (fixed-width field)
        writeFixedString(data, DatabaseConfig.USERNAME_OFFSET,
                row.getUsername(), DatabaseConfig.MAX_USERNAME_LENGTH);

        // Write email (fixed-width field)
        writeFixedString(data, DatabaseConfig.EMAIL_OFFSET,
                row.getEmail(), DatabaseConfig.MAX_EMAIL_LENGTH);

        return data;
    }

    /**
     * Deserializes a row from a byte array.
     *
     * @param data The byte array containing row data
     * @param offset The offset in the array where the row data starts
     * @return The deserialized row, or null if the data represents an empty row
     */
    public static Row deserializeRow(byte[] data, int offset) {
        if (data == null || offset < 0 || offset + DatabaseConfig.ROW_SIZE > data.length) {
            return null;
        }

        // Read ID
        int id = readInt(data, offset + DatabaseConfig.ID_OFFSET);

        // Skip empty rows
        if (id <= 0) {
            return null;
        }

        // Read username
        String username = readFixedString(data, offset + DatabaseConfig.USERNAME_OFFSET,
                DatabaseConfig.MAX_USERNAME_LENGTH);

        // Read email
        String email = readFixedString(data, offset + DatabaseConfig.EMAIL_OFFSET,
                DatabaseConfig.MAX_EMAIL_LENGTH);

        return new Row(id, username, email);
    }

    /**
     * Writes an integer to a byte array in little-endian format.
     *
     * @param data The byte array
     * @param offset The offset to write at
     * @param value The integer value
     */
    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /**
     * Reads an integer from a byte array in little-endian format.
     *
     * @param data The byte array
     * @param offset The offset to read from
     * @return The integer value
     */
    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    /**
     * Writes a string to a fixed-width field in a byte array.
     *
     * @param data The byte array
     * @param offset The offset to write at
     * @param str The string to write
     * @param fieldSize The size of the field
     */
    private static void writeFixedString(byte[] data, int offset, String str, int fieldSize) {
        // Clear the field first
        for (int i = 0; i < fieldSize; i++) {
            data[offset + i] = 0;
        }

        // Write the string if not null/empty
        if (StringUtils.isNotEmpty(str)) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            int bytesToWrite = Math.min(bytes.length, fieldSize - 1); // Leave room for null terminator
            System.arraycopy(bytes, 0, data, offset, bytesToWrite);
        }
    }

    /**
     * Reads a string from a fixed-width field in a byte array.
     *
     * @param data The byte array
     * @param offset The offset to read from
     * @param fieldSize The size of the field
     * @return The string value
     */
    private static String readFixedString(byte[] data, int offset, int fieldSize) {
        // Find the end of the string (null terminator or end of field)
        int endPos = 0;
        while (endPos < fieldSize && data[offset + endPos] != 0) {
            endPos++;
        }

        if (endPos == 0) {
            return "";
        }

        return new String(data, offset, endPos, StandardCharsets.UTF_8).trim();
    }
}