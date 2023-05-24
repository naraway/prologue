/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.spacekeeper;

import io.naraway.accent.domain.context.StageContext;
import io.naraway.accent.domain.context.StageRequest;
import io.naraway.accent.util.json.JsonUtil;
import io.naraway.prologue.autoconfigure.PrologueProperties;
import io.naraway.prologue.security.context.StageContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class SpaceAuthenticationFilter implements Filter {
    //
    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    private final PrologueProperties properties;
    private final StageContextBuilder requestBuilder;
    private final PublicResourceEndPointHolder endPointHolder;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        try {
            String uri = servletRequest.getRequestURI();
            HttpMethod method = HttpMethod.resolve(servletRequest.getMethod());
            if (method == null) {
                method = HttpMethod.POST;
            }

            if (this.endPointHolder.isPublic(uri, method)
                    || this.endPointHolder.isAnonymous(uri, method)
                    || isPermit()) {
                boolean isPublic = this.endPointHolder.isPublic(uri, method);
                boolean isAnonymous = this.endPointHolder.isAnonymous(uri, method);

                if (log.isTraceEnabled()) {
                    log.trace("Request permit uri = {}, method = {}, isPublic = {}, isAnonymous = {}, isPermit = {}",
                            uri, method, isPublic, isAnonymous, isPermit());
                }
            } else {
                boolean authorized = authorized();
                this.requestBuilder.buildRequest(servletRequest);
                StageRequest stageRequest = StageContext.get();
                boolean stageRequestAuthorized = !(stageRequest == null || !stageRequest.hasAuthority());

                if (log.isTraceEnabled()) {
                    log.trace("Request uri = {}, method = {}, authorized = {}, stageRequestAuthorized = {}",
                            uri, method, authorized, stageRequestAuthorized);
                }

                if (!authorized) {
                    log.debug("Unauthorized");
                    throw new AuthenticationCredentialsNotFoundException("Unauthorized");
                }

                if (!stageRequestAuthorized) {
                    log.debug("Requested space is not authorized");
                    throw new AccessDeniedException("Requested space is not authorized");
                }
            }

            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.warn("Error while filter chain", e);
            throw e;
        } finally {
            this.requestBuilder.clearRequest();
        }
    }

    private boolean authorized() {
        //
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.isAuthenticated();
    }

    private boolean isPermit() {
        //
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ANONYMOUS.equals(authority.getAuthority()));
    }

    private Authentication getAuthentication() {
        //
        if (SecurityContextHolder.getContext() == null || SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (log.isTraceEnabled()) {
            log.trace("Authentication = {}", authentication == null ? "NONE" : JsonUtil.toPrettyJson(authentication));
        }

        return authentication;
    }
}
