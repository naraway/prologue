/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.support;

import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnMissingBean(DramaHeaderConsumerProvider.class)
@Getter
public class DramaHeaderConsumerProvider {
    //
    private final Consumer<HttpHeaders> consumer;

    public DramaHeaderConsumerProvider() {
        //
        this.consumer = new HeaderConsumer();
    }
}
