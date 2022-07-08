/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.support;

import io.naraway.accent.domain.trail.TrailContext;
import io.naraway.accent.domain.trail.TrailInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

public class HeaderConsumer implements Consumer<HttpHeaders> {
    //
    private static final String ACTOR_ID_KEY = "actorId";
    private static final String TRAIL_HEADER = "trail";
    private static final List<String> HEADER_KEYS = Arrays.asList(
            ACTOR_ID_KEY,
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            TRAIL_HEADER
    );

    @Override
    public void accept(HttpHeaders httpHeaders) {
        //
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            TrailInfo trailInfo = TrailContext.get();
            if (trailInfo != null) {
                httpHeaders.add(TRAIL_HEADER, trailInfo.toJson());
            }
            return;
        }

        HttpServletRequest request = requestAttributes.getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (HEADER_KEYS.stream().anyMatch(key -> key.equalsIgnoreCase(headerName))) {
                httpHeaders.add(headerName, request.getHeader(headerName));
            }
            httpHeaders.add(headerName, request.getHeader(headerName));
        }
    }
}
