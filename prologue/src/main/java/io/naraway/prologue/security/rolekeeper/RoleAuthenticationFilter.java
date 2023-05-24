/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.rolekeeper;

import io.naraway.accent.domain.context.StageContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class RoleAuthenticationFilter implements Filter {
    //
    private static final List<String> PERMIT_ALL_ANT_MATCHER_PATTERN = Arrays.asList(
            "/images/**",
            "/js/**",
            "/webjars/**",
            "/actuator/**",
            "/swagger-ui.html**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v2/api-docs/**",
            "/v3/api-docs/**");

    private final ResourceRoleEndPointHolder endPointHolder;

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

            boolean isPublic = isPermitAll(uri)
                    || this.endPointHolder.isPublic(uri, method);
            boolean isAnonymous = this.endPointHolder.isAnonymous(uri, method);
            boolean authorized = authorized();

            if (log.isTraceEnabled()) {
                log.trace("Request uri = {}, method = {}, isPublic = {}, isAnonymous = {}",
                        uri, method, isPublic, isAnonymous);
            }

            // check authorized role
            if (!(isPublic || isAnonymous) && authorized) {
                Set<String> requestRoles = new HashSet<>(
                        CollectionUtils.isEmpty(StageContext.get().getRoles())
                                ? Collections.emptyList() : StageContext.get().getRoles());

                if (!this.endPointHolder.hasResourceRole(uri, method, requestRoles)) {
                    throw new AccessDeniedException("Not enough role for requested resource");
                }

                if (!this.endPointHolder.hasMessageRole(uri, method, requestRoles)) {
                    throw new AccessDeniedException("Not enough role for requested message");
                }
            }

            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.warn("Error while filter chain", e);
            throw e;
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

    private Authentication getAuthentication() {
        //
        if (SecurityContextHolder.getContext() == null || SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }

        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isPermitAll(String url) {
        //
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        return PERMIT_ALL_ANT_MATCHER_PATTERN.stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, url));
    }
}
