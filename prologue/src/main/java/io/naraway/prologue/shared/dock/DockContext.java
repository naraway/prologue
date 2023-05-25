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

        String value = this.dockCache.getCache(key);
        if (value != null) {
            return Dock.fromJson(value);
        }

        try {
            this.dockCache.requestLock(key);
            value = this.dockCache.getCache(key); // double check
            if (value == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Dock cache get for citizen, key = {}", key);
                }
                Dock dock = this.dockProxy.findDock(citizen.getId());

                if (dock == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Dock not found");
                    }

                    return null;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Active dock found, dock = {}", dock.toPrettyJson());
                }

                value = dock.toJson();
                this.dockCache.setCache(key, value);
            }

            return Dock.fromJson(value);
        } finally {
            this.dockCache.releaseLock(key);
        }
    }

    public String getOsid(CitizenKey citizen) {
        //
        String key = String.format("osid_%s", citizen.getId());

        String value = this.dockCache.getCache(key);
        if (value != null) {
            return value;
        }

        try {
            this.dockCache.requestLock(key);
            value = this.dockCache.getCache(key); // double check
            if (value == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Osid cache get for citizen, key = {}", key);
                }
                String osid = this.dockProxy.findOsid(citizen.getId());

                if (osid == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Osid not found");
                    }

                    return null;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Osid found, osid = {}", osid);
                }

                value = osid;
                this.dockCache.setCache(key, value);
            }

            return value;
        } finally {
            this.dockCache.releaseLock(key);
        }
    }

    public String getUsid(CitizenKey citizen) {
        //
        String key = String.format("usid_%s", citizen.getId());

        String value = this.dockCache.getCache(key);
        if (value != null) {
            return value;
        }

        try {
            this.dockCache.requestLock(key);
            value = this.dockCache.getCache(key); // double check
            if (value == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Usid cache get for citizen, key = {}", key);
                }
                String usid = this.dockProxy.findUsid(citizen.getId());

                if (usid == null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Usid not found");
                    }

                    return null;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Usid found, usid = {}", usid);
                }

                value = usid;
                this.dockCache.setCache(key, value);
            }

            return value;
        } finally {
            this.dockCache.releaseLock(key);
        }
    }
}
