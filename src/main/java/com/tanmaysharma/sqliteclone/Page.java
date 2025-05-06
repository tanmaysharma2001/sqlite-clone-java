package com.tanmaysharma.sqliteclone;

public class Page {
    private byte[] data;

    public Page(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
