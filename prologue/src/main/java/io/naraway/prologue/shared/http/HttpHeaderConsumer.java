/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */
package io.naraway.prologue.shared.http;

import io.naraway.prologue.shared.auth.InternalAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class HttpHeaderConsumer implements Consumer<HttpHeaders> {
    //
    private static final String ACTOR_ID_KEY = "actorId";
    private static final List<String> INJECTABLE_HEADER_KEYS = Arrays.asList(
            ACTOR_ID_KEY,
            HttpHeaders.AUTHORIZATION.toLowerCase(),
            HttpHeaders.COOKIE.toLowerCase());

    private InternalAuthProvider internalAuthProvider;

    public void setInternalAuthProvider(InternalAuthProvider internalAuthProvider) {
        //
        this.internalAuthProvider = internalAuthProvider;
    }

    @Override
    public void accept(HttpHeaders activeHttpHeaders) {
        //
        ServletRequestAttributes currentRequestAttribute =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (currentRequestAttribute != null) {
            HttpServletRequest currentRequest = currentRequestAttribute.getRequest();
            Enumeration<String> currentRequestHeaderNames = currentRequest.getHeaderNames();
            Collections.list(currentRequestHeaderNames)
                    .forEach(requestHeaderName -> {
                        if (INJECTABLE_HEADER_KEYS.contains(requestHeaderName.toLowerCase()) &&
                                !activeHttpHeaders.containsKey(requestHeaderName.toLowerCase())) {
                            activeHttpHeaders.add(
                                    requestHeaderName.toLowerCase(),
                                    currentRequest.getHeader(requestHeaderName));
                        }
                    });
        }

        if (!activeHttpHeaders.containsKey(HttpHeaders.AUTHORIZATION.toLowerCase()) &&
                this.internalAuthProvider != null && this.internalAuthProvider.getToken() != null) {
            activeHttpHeaders.add(
                    HttpHeaders.AUTHORIZATION.toLowerCase(),
                    "Bearer ".concat(internalAuthProvider.getToken()));
        }

        if (log.isTraceEnabled()) {
            StringBuilder headers = new StringBuilder("========= [Enhanced Headers] ============");
            activeHttpHeaders.forEach((key, value)
                    -> headers.append("\n\t").append(key).append(": ").append(value.toString()));
            headers.append("\n").append("=========================================").append("\n");
            log.trace("\n{}", headers);
        }
    }
}
