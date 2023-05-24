package io.naraway.prologue.security.web.trace;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

@Slf4j
public class TraceFilter implements Filter {
    //
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        if (log.isTraceEnabled()) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;

            Enumeration<String> headerNames = servletRequest.getHeaderNames();
            if (headerNames != null) {
                StringBuilder headers = new StringBuilder()
                        .append("========= [ TRACE: REQUEST HEADERS ] ============");
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    headers.append("\n  ").append(headerName).append(": ").append(servletRequest.getHeader(headerName));
                }
                headers
                        .append("\n")
                        .append("=================================================")
                        .append("\n");
                log.trace("\n{}", headers);
            }
        }

        chain.doFilter(request, response);
    }
}
