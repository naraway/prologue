/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.spacekeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
public class PublicResourceMatcher implements RequestMatcher {
    //
    private final PublicResourceEndPointHolder endPointHolder;

    @Override
    public boolean matches(HttpServletRequest request) {
        //
        String uri = request.getRequestURI();
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (method == null) {
            method = HttpMethod.POST;
        }

        if (endPointHolder.isPublic(uri, method)) {
            if (log.isTraceEnabled()) {
                log.trace("Permit all using AuthorizedRole(isPublic=true), uri = {}, method = {}", uri, method);
            }
            return true;
        }

        return false;
    }
}
