package io.naraway.prologue.support.rest;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

@Data
@Builder
public class RestRequesterProperties {
    //
    private String baseUrl;
    @Builder.Default
    private boolean loopback = false;
    @Builder.Default
    private HttpMethod baseMethod = HttpMethod.POST;
    @Builder.Default
    private int retry = 3;
    @Builder.Default
    private int requestTimeoutSeconds = 60;
    @Builder.Default
    private int maxMemorySize = 1024 * 1024 * 50; // 50 MB
}
