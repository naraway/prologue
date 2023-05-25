/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.shared.dock.cache;

import io.naraway.prologue.autoconfigure.PrologueProperties;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.events.CacheEventListenerConfiguration;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.expiry.ExpiryPolicy;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class DockEhCache implements DockCache {
    //
    private static final ConcurrentMap<String, ReentrantLock> lockManager = new ConcurrentHashMap<>();

    private final CacheManager cacheManager;
    private final Cache<String, String> dockCache;

    public DockEhCache(PrologueProperties properties) {
        //
        PrologueProperties.DockProperties props = properties.getDock();

        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        this.cacheManager.init();

        ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder
                .timeToLiveExpiration(Duration.ofSeconds(props.getCacheTtlSeconds()));

        CacheEventListener<String, String> cacheEventListener = (CacheEventListener) event -> {
            String key = (String) event.getKey();

            if (lockManager.containsKey(key)) {
                ReentrantLock lock = lockManager.get(key);
                if (lock != null && lock.isLocked()) {
                    lock.unlock();
                }
                lockManager.remove(key);
            }
        };

        CacheEventListenerConfiguration<Void> cacheEventListenerConfiguration = CacheEventListenerConfigurationBuilder
                .newEventListenerConfiguration(cacheEventListener, EventType.REMOVED, EventType.EXPIRED).build();

        CacheConfiguration<String, String> cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, String.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(50, MemoryUnit.MB))
                .withExpiry(expiryPolicy)
                .withService(cacheEventListenerConfiguration).build();

        this.dockCache = this.cacheManager.createCache("prologue-dock", cacheConfiguration);

        log.info("Ehcache was configured");
    }

    @Override
    public void setCache(String key, String value) {
        //
        dockCache.put(key, value);
    }

    @Override
    public String getCache(String key) {
        //
        return dockCache.get(key);
    }

    @Override
    public void removeCache(String key) {
        //
        if (dockCache.containsKey(key)) {
            dockCache.remove(key);
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
