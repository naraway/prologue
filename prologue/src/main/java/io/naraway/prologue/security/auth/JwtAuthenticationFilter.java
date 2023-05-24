package io.naraway.prologue.security.auth;

import io.naraway.prologue.security.auth.jwt.JwtSupport;
import io.naraway.prologue.security.spacekeeper.PublicResourceEndPointHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {
    //
    private final JwtSupport jwtSupport;
    private final PublicResourceEndPointHolder endPointHolder;
    private final String[] getIgnoreUrls;
    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        String uri = servletRequest.getRequestURI();
        HttpMethod method = HttpMethod.resolve(servletRequest.getMethod());
        if (method == null) {
            method = HttpMethod.POST;
        }

        boolean isPermit = isIgnoreSecurityURI(uri);
        boolean isPublic = this.endPointHolder.isPublic(uri, method);

        if (!isPermit && !isPublic && this.jwtSupport.validate(servletRequest)) {
            Authentication authentication = this.jwtSupport.resolveAuthentication(servletRequest);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private boolean isIgnoreSecurityURI(String path) {
        //
        return Arrays.stream(getIgnoreUrls).anyMatch(ignoreUtl -> pathMatcher.match(ignoreUtl, path));
    }
}