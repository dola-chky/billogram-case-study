package com.dola.billogramdataprocessingapp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecordType {
    POSITIVE,
    NEGATIVE;

    @JsonCreator
    public static RecordType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return RecordType.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
