package io.naraway.prologue.security.web.xss.converter;

public enum XssConverterType {
    //
    REMOVE("remove"),
    ESCAPE("escape");

    private final String type;

    XssConverterType(String type) {
        //
        this.type = type;
    }

    public String type() {
        //
        return this.type;
    }

    public static XssConverterType from(String value) {
        //
        for (XssConverterType type : XssConverterType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return ESCAPE;
    }
}
