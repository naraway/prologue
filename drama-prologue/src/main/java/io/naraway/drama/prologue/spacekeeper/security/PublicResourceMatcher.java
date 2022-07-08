/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@Slf4j
public class PublicResourceMatcher implements RequestMatcher {
    //
    private final PublicResourceEndPointHolder endPointHolder;

    @Override
    public boolean matches(HttpServletRequest request) {
        //
        String requestUri = request.getRequestURI();

        if (endPointHolder.isPublic(requestUri)) {
            log.trace("Permit all using AuthorizedRole(isPublic=true), uri = {}", requestUri);
            return true;
        }

        return false;
    }
}
