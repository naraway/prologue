package io.naraway.prologue.autoconfigure;

import io.naraway.prologue.shared.auth.InternalAuthProvider;
import io.naraway.prologue.shared.http.HttpHeaderProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = InternalAuthProviderAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@RequiredArgsConstructor
public class HttpHeaderProviderAutoConfiguration {
    //
    private final PrologueProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public HttpHeaderProvider httpHeaderProvider(
            @Autowired(required = false) InternalAuthProvider internalAuthProvider) {
        //
        if (this.properties.getInternalAuth().isEnabled()) {
            return new HttpHeaderProvider(internalAuthProvider);
        } else {
            return new HttpHeaderProvider();
        }
    }
}
