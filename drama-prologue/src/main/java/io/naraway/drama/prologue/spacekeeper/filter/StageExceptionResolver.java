/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter;

import io.naraway.accent.domain.trail.FailureMessage;
import io.naraway.accent.util.json.JsonUtil;
import io.naraway.drama.prologue.spacekeeper.filter.support.CachedBodyHttpServletRequest;
import io.naraway.drama.prologue.spacekeeper.filter.support.RequestedCommand;
import io.naraway.drama.prologue.spacekeeper.filter.support.RequestedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class StageExceptionResolver extends OncePerRequestFilter {
    //
    private static final String X_RESULT = "X-Result";
    private static final String X_EXCEPTION_MESSGE = "X-Exception-Message";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException {
        //
        HttpServletRequest currentdRequest = request;
        if (HttpMethod.valueOf(request.getMethod()) == HttpMethod.POST
                && request.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_JSON_VALUE)) {
            currentdRequest = new CachedBodyHttpServletRequest(request);
        }

        try {
            chain.doFilter(currentdRequest, response);
            response.setHeader(X_RESULT, "true");
        } catch (IOException | ServletException e) {
            this.handleException(e, currentdRequest, response);
        }
    }

    @SuppressWarnings("java:S3740")
    private void handleException(Throwable exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        //
        Throwable currentException = exception;
        if (exception.getCause() != null) {
            currentException = exception.getCause();
        }

        if (currentException instanceof AuthenticationException ||
                AuthenticationException.class.isAssignableFrom(currentException.getClass())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (currentException instanceof AccessDeniedException ||
                AccessDeniedException.class.isAssignableFrom(currentException.getClass()) ||
                currentException instanceof SecurityException ||
                SecurityException.class.isAssignableFrom(currentException.getClass())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (currentException instanceof NoSuchElementException ||
                NoSuchElementException.class.isAssignableFrom(currentException.getClass())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        String requestedJson = getRequestedJson(request);
        if (requestedJson != null && request.getRequestURI().contains("/query")) {
            RequestedQuery query = JsonUtil.fromJson(requestedJson, RequestedQuery.class);
            query.setResponse(new FailureMessage(currentException));

            try (ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response)) {
                serverResponse.getBody().write(JsonUtil.toJson(query.getResponse()).getBytes());
            }
        } else if (requestedJson != null && request.getRequestURI().contains("/command")) {
            RequestedCommand command = JsonUtil.fromJson(requestedJson, RequestedCommand.class);
            command.setResponse(new FailureMessage(currentException));

            try (ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response)) {
                serverResponse.getBody().write(JsonUtil.toJson(command.getResponse()).getBytes());
            }
        } else {
            response.setHeader(X_RESULT, "false");
            response.setHeader(
                    X_EXCEPTION_MESSGE,
                    Optional.ofNullable(currentException.getMessage())
                            .orElse("Unknown exception occurred"));
            try (ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response)) {
                serverResponse.getBody().write(JsonUtil.toJson(new FailureMessage(currentException)).getBytes());
            }
        }
    }

    private String getRequestedJson(HttpServletRequest request) throws IOException {
        //
        if (HttpMethod.valueOf(request.getMethod()) == HttpMethod.POST
                && request.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_JSON_VALUE)) {
            return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8.name());
        }

        return null;
    }
}
