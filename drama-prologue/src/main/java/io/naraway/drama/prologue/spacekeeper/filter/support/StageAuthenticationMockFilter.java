/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter.support;

import io.naraway.accent.domain.key.stage.ActorKey;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequestBuilder;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequestContext;
import io.naraway.drama.prologue.spacekeeper.filter.drama.DramaRequest;
import io.naraway.drama.prologue.spacekeeper.security.PublicResourceEndPointHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class StageAuthenticationMockFilter extends OncePerRequestFilter {
    //
    private final StageRequestBuilder requestBuilder;
    private final PublicResourceEndPointHolder endPointHolder;
    private final DramaRequest sampleDramaRequest;

    @SuppressWarnings("java:S1874")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        String actorId = StringUtils.hasText(request.getHeader(DramaRequest.ACTOR_ID_NAME))
                ? request.getHeader(DramaRequest.ACTOR_ID_NAME)
                : sampleDramaRequest.getActorId();
        List<String> roles = StringUtils.hasText(request.getHeader(DramaRequest.ROLE_NAME))
                ? Arrays.asList(request.getHeader(DramaRequest.ROLE_NAME).split(","))
                : sampleDramaRequest.getRoles();

        DramaRequest dramaRequest = new DramaRequest(
                actorId,
                sampleDramaRequest.getLoginId(),
                sampleDramaRequest.getDisplayName(),
                ActorKey.fromId(actorId).genCitizenId(),
                sampleDramaRequest.getCineroomIds(),
                roles);

        StageRequestContext.set(dramaRequest);

        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.warn("Error while filter chain", e);
            throw e;
        } finally {
            requestBuilder.clearRequest();
        }
    }
}
