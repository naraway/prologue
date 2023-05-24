/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.autoconfigure;

import io.naraway.prologue.shared.auth.InternalAuthProvider;
import io.naraway.prologue.shared.dock.DockContext;
import io.naraway.prologue.shared.dock.DockProxy;
import io.naraway.prologue.shared.dock.DockSession;
import io.naraway.prologue.shared.dock.cache.DockCache;
import io.naraway.prologue.shared.dock.cache.DockEhCache;
import io.naraway.prologue.shared.dock.cache.DockGenericCache;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = InternalAuthProviderAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@ConditionalOnProperty(name = "nara.prologue.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DockAutoConfiguration {
    //
    private final PrologueProperties properties;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.dock.enabled", havingValue = "true", matchIfMissing = true)
    public DockCache dockCache() {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();

        switch (props.getCacheType()) {
            case GENERIC:
                return new DockGenericCache(this.properties);
            case EHCACHE:
                return new DockEhCache(this.properties);
            default:
                return null;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.dock.enabled", havingValue = "true", matchIfMissing = true)
    public DockProxy dockProxy(InternalAuthProvider internalAuthProvider) {
        //
        return new DockProxy(this.properties, internalAuthProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.dock.enabled", havingValue = "true", matchIfMissing = true)
    public DockContext dockContext(DockProxy dockProxy) {
        //
        return new DockContext(dockProxy, dockCache());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.dock.enabled", havingValue = "true", matchIfMissing = true)
    public DockSession dockSession(DockContext dockContext) {
        //
        return new DockSession(dockContext);
    }
}
