/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.shared.dock.cache;

import io.naraway.prologue.autoconfigure.PrologueProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
public class DockGenericCache implements DockCache {
    //
    private static final ConcurrentMap<String, String> dockCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> expiryManager = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ReentrantLock> lockManager = new ConcurrentHashMap<>();

    private final PrologueProperties properties;

    private Timer timer;

    @PostConstruct
    @SuppressWarnings("java:S3776")
    private void initialize() {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();
        this.timer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                Set<String> expiredKeys = new HashSet<>();

                for (Map.Entry<String, Long> entiry : DockGenericCache.expiryManager.entrySet()) {
                    Long expirationTimeMillis = entiry.getValue();
                    if (expirationTimeMillis <= currentTimeMillis) {
                        expiredKeys.add(entiry.getKey());
                    }
                }

                for (String key : expiredKeys) {
                    if (log.isTraceEnabled()) {
                        log.trace("Generic cache expired, key = {}", key);
                    }

                    dockCache.remove(key);
                    expiryManager.remove(key);
                    if (lockManager.containsKey(key)) {
                        ReentrantLock lock = lockManager.get(key);
                        if (lock != null && lock.isLocked()) {
                            lock.unlock();
                        }
                        lockManager.remove(key);
                    }
                }
            }
        };

        this.timer.scheduleAtFixedRate(
                timerTask,
                (long) props.getCacheRefreshIntervalSeconds() * 1000,
                (long) props.getCacheRefreshIntervalSeconds() * 1000);
        log.info("Generic cache was configured");
    }

    @PreDestroy
    private void destroy() {
        //
        if (this.timer != null) {
            this.timer.cancel();
        }
        log.info("Generic cache was destroyed");
    }

    @Override
    public void setCache(String key, String dock) {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();

        long expirationTimeMillis = System.currentTimeMillis() + props.getCacheTtlSeconds() * 1000L;
        dockCache.put(key, dock);
        expiryManager.put(key, expirationTimeMillis);
    }

    @Override
    public String getCache(String key) {
        //
        return dockCache.get(key);
    }

    @Override
    public void removeCache(String key) {
        //
        dockCache.remove(key);
        expiryManager.remove(key);
        if (lockManager.containsKey(key)) {
            ReentrantLock lock = lockManager.get(key);
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
            lockManager.remove(key);
        }
    }

    @Override
    @SuppressWarnings("java:S2222")
    public void requestLock(String key) {
        //
        ReentrantLock reentrantLock = lockManager.computeIfAbsent(key, k -> new ReentrantLock());
        reentrantLock.lock();
    }

    @Override
    public void releaseLock(String key) {
        //
        ReentrantLock reentrantLock = lockManager.computeIfAbsent(key, k -> new ReentrantLock());
        reentrantLock.unlock();
    }
}
