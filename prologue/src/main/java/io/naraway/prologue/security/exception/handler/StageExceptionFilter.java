package io.naraway.prologue.security.exception.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class StageExceptionFilter extends OncePerRequestFilter {
    //
    @Override
    @SuppressWarnings("java:S3776")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException {
        //
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            if (e instanceof NoSuchElementException
                    || NoSuchElementException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else if (e instanceof IllegalArgumentException
                    || IllegalArgumentException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
            } else if (e instanceof AccessDeniedException
                    || AccessDeniedException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
            } else if (e instanceof AuthenticationException
                    || AuthenticationException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            } else if (e instanceof IllegalStateException
                    || IllegalStateException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            } else if (e instanceof JwtException
                    || JwtException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            } else if (e instanceof SecurityException
                    || SecurityException.class.isAssignableFrom(e.getClass())) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
            } else {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }

            HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
            exceptionHttpHeader.forEach((name, value) ->
                    response.setHeader(name, value.stream().findFirst().orElse(null)));

            FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
            response.getWriter().write(failureResponse.toJson());

            StageExceptions.clearStageExceptionContext();
        }
    }
}
