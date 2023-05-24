/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.context;

import io.naraway.accent.domain.context.StageContext;
import io.naraway.accent.domain.context.StageRequest;
import io.naraway.accent.domain.context.UserType;
import io.naraway.accent.domain.tenant.ActorKey;
import io.naraway.accent.domain.tenant.TenantKey;
import io.naraway.accent.domain.tenant.TenantType;
import io.naraway.accent.util.json.JsonUtil;
import io.naraway.prologue.security.auth.jwt.JwtNames;
import io.naraway.prologue.security.auth.jwt.JwtSupport;
import io.naraway.prologue.shared.dock.DockSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class StageContextBuilder {
    //
    public static final String SCOPE_INTERNAL = "internal";
    public static final String SCOPE_SERVICE = "service";
    public static final String ROLE_INTERNAL = "_internal_";
    public static final String ROLES = "roles";
    public static final String KOLLECTION_ID = "kollectionId";
    public static final String ACTOR_ID = "actorId";

    @Value("${spring.application.name:}")
    private String drama;

    @Autowired(required = false)
    private DockSession dockSession;

    private final JwtSupport jwtSupport;

    @SuppressWarnings("java:S1874")
    public void buildRequest(HttpServletRequest request) {
        //
        validateCitizen(request);

        StageRequest stageRequest = createStageRequest(request);

        if (stageRequest == null) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot create stage request");
            }
        } else {
            StageContext.set(stageRequest);
            if (log.isTraceEnabled()) {
                log.trace("Created stage request = {}", JsonUtil.toJson(stageRequest));
            }
        }
    }

    @SuppressWarnings("java:S1874")
    public void clearRequest() {
        //
        StageContext.clear();
    }

    StageRequest createStageRequest(HttpServletRequest request) {
        //
        String requestActorId = getRequestActorId(request);
        String pavilionId = getRequestPavilionId(request);
        List<String> requestRoles = getRequestRoles(request);
        String requestKollection = getRequestKollection(request);
        Map<String, Object> claims = this.jwtSupport.getClaims(request);
        String scope = getScope(request);

        if (log.isTraceEnabled()) {
            log.trace("Create drama request, requestActorId = {}, requestRoles = {}, scope = {}, payload = {}",
                    requestActorId, JsonUtil.toJson(requestRoles), scope, JsonUtil.toJson(claims));
        }

        if (!claims.isEmpty()) {
            Map<String, Object> attributes = (Map) claims.get(JwtNames.PARAMETER_ATTRIBUTES);
            if (attributes == null) {
                attributes = Collections.emptyMap();
            }

            String username = getValue(claims, OAuth2ParameterNames.USERNAME);
            String email = getValue(claims, JwtNames.PARAMETER_EMAIL);
            String displayName = getValue(claims, JwtNames.PARAMETER_DISPLAY_NAME);

            if (!StringUtils.hasText(scope)) {
                return StageRequest.builder()
                        .actorId(requestActorId)
                        .username(username)
                        .email(email)
                        .displayName(displayName)
                        .cineroomIds(getValues(attributes, JwtNames.ATTRIBUTES_CINEROOM_IDS))
                        .pavilionId(pavilionId)
                        .kollectionId(requestKollection)
                        .roles(requestRoles)
                        .userType(UserType.Service)
                        .attributes(attributes)
                        .build();
            }

            if (SCOPE_INTERNAL.equals(scope)) {
                return StageRequest.builder()
                        .actorId(requestActorId)
                        .username(username)
                        .email(email)
                        .displayName(displayName)
                        .cineroomIds(Collections.emptyList())
                        .pavilionId(pavilionId)
                        .dramaId(this.drama)
                        .roles(List.of(ROLE_INTERNAL))
                        .userType(UserType.Internal)
                        .attributes(attributes)
                        .build();
            }

            if (SCOPE_SERVICE.equals(scope)) {
                String claimActorId = getServiceActorId(attributes);
                return StageRequest.builder()
                        .actorId(claimActorId)
                        .username(username)
                        .email(email)
                        .displayName(displayName)
                        .cineroomIds(Collections.emptyList())
                        .pavilionId(pavilionId)
                        .dramaId(this.drama)
                        .roles(getValues(attributes, JwtNames.ATTRIBUTES_SERVICE_ROLES))
                        .userType(UserType.Service)
                        .attributes(attributes)
                        .build();
            }

            return StageRequest.builder()
                    .actorId(requestActorId)
                    .username(username)
                    .email(email)
                    .displayName(displayName)
                    .cineroomIds(getValues(attributes, JwtNames.ATTRIBUTES_CINEROOM_IDS))
                    .pavilionId(pavilionId)
                    .kollectionId(requestKollection)
                    .dramaId(this.drama)
                    .roles(requestRoles)
                    .userType(UserType.Citizen)
                    .attributes(attributes)
                    .build();
        }

        return null;
    }

    private String getRequestActorId(HttpServletRequest request) {
        //
        if (StringUtils.hasText(request.getHeader(StageContextBuilder.ACTOR_ID))) {
            String actorId = request.getHeader(StageContextBuilder.ACTOR_ID);
            if (TenantKey.getTenantType(actorId) == TenantType.Actor) {
                return actorId;
            }
        }

        return StageRequest.anonymous().getActorId();
    }

    private String getServiceActorId(Map<String, Object> attributes) {
        //
        String actorIdName = "actor_id";
        String stageIdName = "stage_id";
        String cineroomIdName = "cineroom_id";
        String pavilionIdName = "pavilion_id";

        if (attributes.containsKey(actorIdName)) {
            return getValue(attributes, actorIdName);
        }

        if (attributes.containsKey(stageIdName)) {
            return String.format("0@%s", getValue(attributes, stageIdName));
        }

        if (attributes.containsKey(cineroomIdName)) {
            return String.format("0@%s-0", getValue(attributes, cineroomIdName));
        }

        if (attributes.containsKey(pavilionIdName)) {
            return String.format("0@%s:0-0", getValue(attributes, pavilionIdName));
        }

        return StageRequest.anonymous().getActorId();
    }

    private String getRequestPavilionId(HttpServletRequest request) {
        //
        String actorId = getRequestActorId(request);

        return ActorKey.fromId(actorId).genPavilionId();
    }

    private String getCitizenId(HttpServletRequest request) {
        //
        Map<String, Object> claims = this.jwtSupport.getClaims(request);

        if (claims != null && claims.containsKey(JwtNames.PARAMETER_ATTRIBUTES)) {
            Map<String, Object> attributes = (Map<String, Object>) claims.get(JwtNames.PARAMETER_ATTRIBUTES);

            return (String) attributes.get(JwtNames.ATTRIBUTES_CITIZEN_ID);
        }

        return null;
    }

    private void validateCitizen(HttpServletRequest request) {
        //
        if (isInternalRequest(request)) {
            return;
        }

        String actorId = getRequestActorId(request);
        String citizenId = getCitizenId(request);

        if (citizenId == null || actorId.equals(StageRequest.anonymous().getActorId())) {
            throw new AccessDeniedException("Request actor was not identified with token.");
        }
    }

    private String getJti(HttpServletRequest request) {
        //
        Map<String, Object> claims = this.jwtSupport.getClaims(request);

        if (claims != null && claims.containsKey(JwtClaimNames.JTI)) {
            return (String) claims.get(JwtClaimNames.JTI);
        }

        return null;
    }

    private List<String> getRequestRoles(HttpServletRequest request) {
        //
        String scope = getScope(request);

        if (this.dockSession == null) {
            if (log.isTraceEnabled()) {
                log.trace("DockSession is not activated, use header roles");
            }
            if (StringUtils.hasText(request.getHeader(ROLES))) {
                if (log.isTraceEnabled()) {
                    log.trace("Parsed request drama roles = {}", request.getHeader(ROLES));
                }
                return Arrays.asList(request.getHeader(ROLES).split(","));
            }
            if (log.isTraceEnabled()) {
                log.trace("No request drama role");
            }
            return Collections.emptyList();
        }

        // for local development
        if (isInternalRequest(request) && SCOPE_INTERNAL.equals(scope)) {
            return Collections.emptyList();
        }

        String jti = getJti(request);
        String requestActorId = getRequestActorId(request);
        String refererUrl = request.getHeader(HttpHeaders.REFERER);

        List<String> roles = this.dockSession.getRoles(requestActorId, this.drama, refererUrl, jti);

        return CollectionUtils.isEmpty(roles) ? Collections.emptyList() : roles;
    }

    private String getRequestKollection(HttpServletRequest request) {
        //
        String scope = getScope(request);

        if (this.dockSession == null) {
            if (log.isTraceEnabled()) {
                log.trace("DockSession is not activated, use header kollectionId");
            }
            if (StringUtils.hasText(request.getHeader(KOLLECTION_ID))) {
                if (log.isTraceEnabled()) {
                    log.trace("Parsed request kollection id = {}", request.getHeader(KOLLECTION_ID));
                }
                return request.getHeader(KOLLECTION_ID);
            }
            if (log.isTraceEnabled()) {
                log.trace("No request kollection id");
            }
            return null;
        }

        // for local development
        if (isInternalRequest(request) && SCOPE_INTERNAL.equals(scope)) {
            return null;
        }

        String jti = getJti(request);
        String requestActorId = getRequestActorId(request);
        String refererUrl = request.getHeader(HttpHeaders.REFERER);

        String kollection = this.dockSession.getKollection(requestActorId, this.drama, refererUrl, jti);

        return !StringUtils.hasText(kollection) ? null : kollection;
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        //
        String refererUrl = request.getHeader(HttpHeaders.REFERER);
        return !StringUtils.hasText(refererUrl) || refererUrl.startsWith("http://localhost");
    }

    private String getScope(HttpServletRequest request) {
        //
        Map<String, Object> claims = this.jwtSupport.getClaims(request);

        if (claims != null && claims.containsKey(OAuth2ParameterNames.SCOPE)) {
            List<String> scopes = (List<String>) claims.get(OAuth2ParameterNames.SCOPE);
            return scopes.stream().findFirst().orElseGet(() -> "");
        }

        return "";
    }

    private String getValue(Map<String, Object> attributes, String name) {
        //
        String value = (String) attributes.get(name);

        return value == null ? "" : value;
    }

    private List<String> getValues(Map<String, Object> attributes, String name) {
        //
        List<String> values = (List<String>) attributes.get(name);

        return CollectionUtils.isEmpty(values) ? new ArrayList(Collections.emptyList()) : values;
    }
}
