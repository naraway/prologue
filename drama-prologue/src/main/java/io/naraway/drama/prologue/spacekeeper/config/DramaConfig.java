/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@ComponentScan(basePackages = {
        "io.naraway.drama.prologue.spacekeeper.filter.drama",
        "io.naraway.drama.prologue.spacekeeper.security",
        "io.naraway.drama.prologue.spacekeeper.support"
})
public class DramaConfig {
    //
}
