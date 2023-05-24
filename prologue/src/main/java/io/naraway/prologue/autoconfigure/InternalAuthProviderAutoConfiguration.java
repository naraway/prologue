package io.naraway.prologue.autoconfigure;

import io.naraway.prologue.shared.auth.InternalAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PrologueAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@ConditionalOnProperty(name = "nara.prologue.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class InternalAuthProviderAutoConfiguration {
    //
    private final PrologueProperties properties;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.internal-auth.enabled", havingValue = "true", matchIfMissing = true)
    public InternalAuthProvider internalAuthProvider() {
        //
        return new InternalAuthProvider(this.properties);
    }
}
