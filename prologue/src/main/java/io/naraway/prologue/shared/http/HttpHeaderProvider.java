/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */
package io.naraway.prologue.shared.http;

import io.naraway.prologue.shared.auth.InternalAuthProvider;
import lombok.Getter;
import org.springframework.http.HttpHeaders;

import java.util.function.Consumer;

@Getter
public class HttpHeaderProvider {
    //
    private final Consumer<HttpHeaders> consumer;

    public HttpHeaderProvider() {
        //
        this.consumer = new HttpHeaderConsumer();
    }

    public HttpHeaderProvider(InternalAuthProvider internalAuthProvider) {
        //
        this.consumer = new HttpHeaderConsumer();
        ((HttpHeaderConsumer) consumer).setInternalAuthProvider(internalAuthProvider);
    }

    // injectable consumer
    public HttpHeaderProvider(Consumer<HttpHeaders> consumer) {
        //
        this.consumer = consumer;
    }

    // injectable consumer
    public HttpHeaderProvider(Consumer<HttpHeaders> consumer, InternalAuthProvider internalAuthProvider) {
        //
        this.consumer = consumer;
        ((HttpHeaderConsumer) consumer).setInternalAuthProvider(internalAuthProvider);
    }
}
