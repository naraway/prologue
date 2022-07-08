/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.security;

import io.naraway.accent.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class StageAuthenticationEntryPoint implements AuthenticationEntryPoint {
    //
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException e) throws IOException {
        // 401
        log.error("Authentication exception, message=" + e.getMessage(), e);
        try (ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response)) {
            serverResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
            serverResponse.getServletResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            serverResponse.getBody().write(JsonUtil.toJson(e).getBytes());
        }
    }
}
