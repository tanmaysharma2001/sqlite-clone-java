package com.tanmaysharma.sqliteclone;

public enum MetaCommandResult {
    SUCCESS,
    UNRECOGNIZED_COMMAND
}

public class MetaCommands {
    public static MetaCommandResult doMetaCommand(InputBuffer in, Table table) {
        if (".exit".equals(in.getBuffer())) {
            Database.close(table);
            System.exit(0);
            return MetaCommandResult.SUCCESS;
        }
        return MetaCommandResult.UNRECOGNIZED_COMMAND;
    }
}
