package io.naraway.prologue.shared.dock;

import io.naraway.accent.domain.tenant.CitizenKey;
import io.naraway.prologue.shared.dock.cache.DockCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DockContext {
    //
    private final DockProxy dockProxy;
    private final DockCache dockCache;

    public Dock getDock(CitizenKey citizen, String jti) {
        //
        String key = String.format("%s:%s", citizen.getId(), jti);

        String value = dockCache.getCache(key);
        if (value != null) {
            return Dock.fromJson(value);
        }

        try {
            dockCache.requestLock(key);
            value = dockCache.getCache(key); // double check
            if (value == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Dock cache get for citizen, key = {}", key);
                }
                Dock dock = this.dockProxy.get(citizen.getId());

                if (dock == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Dock not found");
                    }
                    return null;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Active dock found, dock = {}", dock.toPrettyJson());
                }
                return dock;
            }
            return Dock.fromJson(value);
        } finally {
            dockCache.releaseLock(key);
        }
    }
}
