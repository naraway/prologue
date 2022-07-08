/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter;

import io.naraway.drama.prologue.spacekeeper.security.PublicResourceEndPointHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class StageAuthenticationFilter extends OncePerRequestFilter {
    //
    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    private final StageRequestBuilder requestBuilder;
    private final PublicResourceEndPointHolder endPointHolder;
    private final List<String> activeProfiles;
    private final List<String> testProfiles;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        try {
            requestBuilder.buildRequest(request);

            if (!testProfiles.isEmpty() && testProfiles.stream()
                    .anyMatch(activeProfiles::contains)) {
                log.info("Space keeper was disabled by test profiles = {}", activeProfiles);
            } else {
                StageRequest stageRequest = StageRequestContext.get();

                boolean isPublic = endPointHolder.isPublic(request.getRequestURI());
                boolean isAnonymous = endPointHolder.isAnonymous(request.getRequestURI());
                boolean authorized = authorized();
                boolean isPermitAll = isPermitAll();

                log.trace("Request uri = {}, isPublic = {}, isAnonymous = {}, permitAllMatch = {}",
                        request.getRequestURI(), isPublic, isAnonymous, isPermitAll);

                if (!isPublic && !authorized) {
                    log.debug("Unauthorized");
                    throw new AuthenticationCredentialsNotFoundException("Unauthorized");
                }

                if (!(isPublic || isPermitAll)
                        && (stageRequest == null || !stageRequest.hasStageAuthority())
                        && !isAnonymous) {
                    log.debug("Requested space is not authorized");
                    throw new AccessDeniedException("Requested space is not authorized");
                }
            }

            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.warn("Error while filter chain", e);
            throw e;
        } finally {
            requestBuilder.clearRequest();
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

    private boolean isPermitAll() {
        //
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ANONYMOUS.equals(authority.getAuthority()));
    }

    private Authentication getAuthentication() {
        if (SecurityContextHolder.getContext() == null || SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }

        return SecurityContextHolder.getContext().getAuthentication();
    }
}
