/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter.drama;

import io.naraway.accent.domain.key.stage.ActorKey;
import io.naraway.accent.domain.trail.TrailInfo;
import io.naraway.accent.util.json.JsonUtil;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequestBuilder;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequestContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class DramaRequestBuilder implements StageRequestBuilder {
    //
    private static final Base64.Decoder decoder = Base64.getDecoder();

    @SuppressWarnings("java:S1874")
    @Override
    public void buildRequest(HttpServletRequest request) {
        //
        DramaRequest currentRequest = createCurrentRequest(request);
        StageRequestContext.set(currentRequest);
    }

    @SuppressWarnings("java:S1874")
    @Override
    public void buildRequest(TrailInfo trailInfo) {
        //
        DramaRequest currentRequest = createCurrentRequest(trailInfo);
        StageRequestContext.set(currentRequest);
    }

    @SuppressWarnings("java:S1874")
    @Override
    public void clearRequest() {
        //
        StageRequestContext.clear();
    }

    private DramaRequest createCurrentRequest(HttpServletRequest request) {
        //
        String actorId = getCurrentActorId(request);
        List<String> roles = getCurrentRoles(request);
        Map<String, Object> payload = getPayloadFromRequest(request);

        log.trace("Create current request, actorId = {}, payload = {}", actorId, payload);

        if (!CollectionUtils.isEmpty(payload)) {
            return new DramaRequest(
                    actorId,
                    (String) payload.get("loginId"),
                    (String) payload.get("displayName"),
                    StringUtils.hasText(actorId)
                            ? ActorKey.fromId(actorId).genAudienceKey().genCitizenKey().getId() : null,
                    (List<String>) payload.get("cineroomIds"),
                    roles
            );
        }

        return null;
    }

    private DramaRequest createCurrentRequest(TrailInfo trailInfo) {
        //
        if (trailInfo == null) {
            return null;
        }

        String actorId = trailInfo.getUserId();
        String userId = trailInfo.getUserId();
        String citizenId = StringUtils.hasText(actorId)
                ? ActorKey.fromId(actorId).genAudienceKey().genCitizenKey().getId() : null;
        List<String> cineroomIds = StringUtils.hasText(actorId)
                ? Arrays.asList(ActorKey.fromId(actorId).genCineroomId()) : Collections.emptyList();
        List<String> roles = new ArrayList<>();

        log.trace("Create current request, actorId = {}, userId = {}", actorId, userId);

        return new DramaRequest(actorId, userId, null, citizenId, cineroomIds, roles);
    }

    private String getCurrentActorId(HttpServletRequest request) {
        //
        if (request.getHeader(DramaRequest.ACTOR_ID_NAME) != null) {
            return request.getHeader(DramaRequest.ACTOR_ID_NAME);
        }

        return null;
    }

    private List<String> getCurrentRoles(HttpServletRequest request) {
        //
        if (request.getHeader(DramaRequest.ROLE_NAME) != null) {
            return Arrays.asList(request.getHeader(DramaRequest.ROLE_NAME).split(","));
        }

        return Collections.emptyList();
    }

    private Map<String, Object> getPayloadFromRequest(HttpServletRequest request) {
        //
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = authorization.substring("Bearer ".length());

            return JsonUtil.fromJson(getTokenPayload(token), Map.class);
        }

        return Collections.emptyMap();
    }

    private String getTokenPayload(String token) throws IllegalArgumentException {
        //
        String[] chunks = token.split("\\.");

        return new String(decoder.decode(chunks[1]));
    }
}
