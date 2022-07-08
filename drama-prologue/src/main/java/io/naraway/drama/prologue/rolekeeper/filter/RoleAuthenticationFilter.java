/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.rolekeeper.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthenticationFilter extends OncePerRequestFilter {
    //
    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    private static final List<String> PERMIT_ALL_ANT_MATCHER_PATTERN = Arrays.asList(
            "/swagger-ui.html**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/actuator/**",
            "/v2/api-docs/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );
    private final RoleResourceEndPointHolder endPointHolder;
    @Value("${spring.profiles.active:default}")
    private List<String> activeProfiles;
    @Value("${nara.test-profiles:default,k8s-test}")
    private List<String> testProfiles;

    @SuppressWarnings("java:S3776")
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        //
        try {
            if (!testProfiles.isEmpty() && testProfiles.stream()
                    .anyMatch(authIgnoreProfile -> activeProfiles.contains(authIgnoreProfile))) {
                log.info("Role keeper was disabled by test profiles = {}", activeProfiles);
            } else {
                boolean isPublic = isPermitAll(request.getRequestURI()) || endPointHolder.isPublic(request.getRequestURI());
                boolean authorized = authorized();
                boolean isAnonymous = isAnonymous();

                log.trace("Request uri = {}, isPublic = {}, permitAllMatch = {}", request.getRequestURI(), isPublic, isAnonymous);

                // check authorized role
                if (!isPublic && authorized) {
                    Set<String> requestRoles = new HashSet<>();
                    if (StringUtils.hasText(request.getHeader("roles"))) {
                        List<String> roles = Arrays.stream(request.getHeader("roles").split(","))
                                .map(String::trim).collect(Collectors.toList());
                        requestRoles.addAll(roles);
                    }

                    if (!endPointHolder.hasResourceRole(request.getRequestURI(), requestRoles)) {
                        throw new ServletException(new IllegalAccessException("Not enough role for requested resource"));
                    }

                    if (!endPointHolder.hasMessageRole(request.getRequestURI(), requestRoles)) {
                        throw new ServletException(new IllegalAccessException("Not enough role for requested message"));
                    }
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

    private boolean isAnonymous() {
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

        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isPermitAll(String url) {
        //
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        return PERMIT_ALL_ANT_MATCHER_PATTERN.stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, url));
    }
}
