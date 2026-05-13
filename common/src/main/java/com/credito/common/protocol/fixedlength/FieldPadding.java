package com.credito.common.protocol.fixedlength;

public enum FieldPadding {
    SPACE(' '),
    ZERO('0');

    private final char value;

    FieldPadding(char value) {
        this.value = value;
    }

    char value() {
        return value;
    }
}
