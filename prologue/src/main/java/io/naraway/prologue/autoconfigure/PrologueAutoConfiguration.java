/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.autoconfigure;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@EnableConfigurationProperties(PrologueProperties.class)
@RequiredArgsConstructor
public class PrologueAutoConfiguration {
    //
    private final PrologueProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        //
        return new PropertySourcesPlaceholderConfigurer();
    }
}
