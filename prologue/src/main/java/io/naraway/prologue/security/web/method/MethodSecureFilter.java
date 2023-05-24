package io.naraway.prologue.security.web.method;

import io.naraway.prologue.autoconfigure.PrologueProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MethodSecureFilter implements Filter {
    //
    private final boolean enabled;
    private final List<String> deniedMethods;

    public MethodSecureFilter(PrologueProperties properties) {
        //
        this.enabled = properties.getMethodSecure().isEnabled();
        this.deniedMethods = CollectionUtils.isEmpty(properties.getMethodSecure().getMethods())
                ? Collections.emptyList() : properties.getMethodSecure().getMethods();

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;

        if (this.enabled && !CollectionUtils.isEmpty(this.deniedMethods) &&
                this.deniedMethods.contains(servletRequest.getMethod())) {
            servletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            chain.doFilter(request, response);
        }
    }
}
