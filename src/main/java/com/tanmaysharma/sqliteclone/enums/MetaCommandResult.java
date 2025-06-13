package com.tanmaysharma.sqliteclone.enums;

/**
 * Result codes for meta commands (commands starting with dot).
 */
public enum MetaCommandResult {
    META_COMMAND_SUCCESS("Meta command executed successfully"),
    META_COMMAND_UNRECOGNIZED_COMMAND("Unrecognized meta command"),
    META_COMMAND_EXIT("Exit requested");

    private final String description;

    MetaCommandResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}