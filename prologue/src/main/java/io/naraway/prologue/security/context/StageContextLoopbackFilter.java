/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.context;

import io.naraway.accent.domain.context.StageContext;
import io.naraway.accent.domain.context.StageRequest;
import io.naraway.prologue.autoconfigure.PrologueProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class StageContextLoopbackFilter implements Filter {
    //
    @Value("${spring.application.name:}")
    private String drama;

    private final PrologueProperties properties;
    private final StageContextBuilder requestBuilder;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        StageRequest stageRequest = this.requestBuilder.createStageRequest(servletRequest);
        if (stageRequest == null) {
            StageContext.set(defaultRequest());
        } else {
            StageContext.set(stageRequest);
        }

        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.warn("Error while filter chain", e);
            throw e;
        } finally {
            this.requestBuilder.clearRequest();
        }
    }

    private StageRequest defaultRequest() {
        //
        PrologueProperties.StageRequestProperties props = this.properties.getDefaultContext();

        return StageRequest.builder()
                .username(props.getUsername())
                .userType(props.getUserType())
                .displayName(props.getDisplayName())
                .email(props.getEmail())
                .enabled(props.isEnabled())
                .actorId(props.getActorId())
                .pavilionId(props.getPavilionId())
                .cineroomIds(props.getCineroomIds())
                .kollectionId(props.getKollectionId())
                .dramaId(this.drama)
                .roles(props.getRoles())
                .attributes(Collections.emptyMap())
                .build();
    }
}
