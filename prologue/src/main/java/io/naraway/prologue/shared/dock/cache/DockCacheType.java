package io.naraway.prologue.shared.dock.cache;

public enum DockCacheType {
    //
    GENERIC("generic"),
    EHCACHE("ehcache");

    private final String type;

    DockCacheType(String type) {
        //
        this.type = type;
    }

    public String type() {
        //
        return this.type;
    }

    public static DockCacheType from(String value) {
        //
        for (DockCacheType type : DockCacheType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return GENERIC;
    }
}
