package io.naraway.prologue.shared.dock.cache;

public interface DockCache {
    //
    void setCache(String key, String dock);
    String getCache(String key);
    void removeCache(String key);

    void requestLock(String key);
    void releaseLock(String key);
}
