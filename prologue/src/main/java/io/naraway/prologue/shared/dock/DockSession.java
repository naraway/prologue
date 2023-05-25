/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.shared.dock;

import io.naraway.accent.domain.rolemap.DramaRole;
import io.naraway.accent.domain.rolemap.KollectionRole;
import io.naraway.accent.domain.tenant.*;
import io.naraway.accent.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DockSession {
    //
    private final DockContext dockContext;

    @SuppressWarnings("java:S3776")
    public List<String> getRoles(
            String actorId, String drama, String refererUrl, String jti) {
        //
        if (log.isTraceEnabled()) {
            log.trace("Get session roles, actorId = {}, drama = {}, refererUrl = {}, jit = {}",
                    actorId, drama, refererUrl, jti);
        }

        CitizenKey citizenKey = null;
        if (StringUtils.hasText(actorId)) {
            citizenKey = getCitizenKey(actorId);
        }

        if (citizenKey == null || !StringUtils.hasText(drama) || !StringUtils.hasText(refererUrl)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot find role, citizenKey = {}, drama = {}, refererUrl = {}",
                        citizenKey, drama, refererUrl);
            }
            return Collections.emptyList();
        }

        Dock dock = getDock(citizenKey, jti);
        if (dock == null) {
            if (log.isTraceEnabled()) {
                log.trace("No dock found, return empty list");
            }
            return Collections.emptyList();
        }

        List<Dock.UserStage> dockStages = new ArrayList<>();
        dock.getCinerooms().forEach(cineroom -> dockStages.addAll(cineroom.getStages()));

        String stageId = ActorKey.fromId(actorId).genStageId();

        Dock.UserStage stage = dockStages.stream()
                .filter(dockStage -> dockStage.getStage().getId().equals(stageId))
                .findFirst().orElse(null);
        if (stage == null) {
            if (log.isTraceEnabled()) {
                log.trace("No stage found, return empty list");
            }
            return Collections.emptyList();
        }

        Dock.UserKollection kollection = stage.getKollections().stream()
                .filter(dockKollection -> {
                    String path = dockKollection.getPath();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }

                    String urlPath = String.format("/%s", path);
                    String urlFullPath = String.format("/%s/", path);

                    return refererUrl.endsWith(urlPath) || refererUrl.contains(urlFullPath);
                })
                .findFirst().orElse(null);
        if (kollection == null) {
            if (log.isTraceEnabled()) {
                log.trace("No kollection found, return empty list");
            }
            return Collections.emptyList();
        }

        List<String> roles = new ArrayList<>();
        for (KollectionRole kollectionRole : kollection.getKollectionRoles()) {
            for (DramaRole dramaRole : kollectionRole.getDramaRoles()) {
                if (dramaRole.getDramaId().equals(drama)) {
                    roles.add(dramaRole.getCode());
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Found session roles, actor = {}, drama = {}, refererUrl = {}, roles = {}",
                    actorId, drama, refererUrl, JsonUtil.toJson(roles));
        }

        return roles;
    }

    @SuppressWarnings("java:S3776")
    public String getKollection(
            String actorId, String drama, String refererUrl, String jti) {
        //
        if (log.isTraceEnabled()) {
            log.trace("Get session kollection, actorId = {}, drama = {}, refererUrl = {}, jit = {}",
                    actorId, drama, refererUrl, jti);
        }

        CitizenKey citizenKey = null;
        if (StringUtils.hasText(actorId)) {
            citizenKey = getCitizenKey(actorId);
        }

        if (citizenKey == null || !StringUtils.hasText(drama) || !StringUtils.hasText(refererUrl)) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot find kollection, citizenKey = {}, drama = {}, refererUrl = {}",
                        citizenKey, drama, refererUrl);
            }
            return null;
        }

        Dock dock = getDock(citizenKey, jti);
        if (dock == null) {
            if (log.isTraceEnabled()) {
                log.trace("No dock found, return null");
            }
            return null;
        }

        List<Dock.UserStage> dockStages = new ArrayList<>();
        dock.getCinerooms().forEach(cineroom -> dockStages.addAll(cineroom.getStages()));

        String stageId = ActorKey.fromId(actorId).genStageId();

        Dock.UserStage stage = dockStages.stream()
                .filter(dockStage -> dockStage.getStage().getId().equals(stageId))
                .findFirst().orElse(null);
        if (stage == null) {
            if (log.isTraceEnabled()) {
                log.trace("No stage found, return null");
            }
            return null;
        }

        Dock.UserKollection kollection = stage.getKollections().stream()
                .filter(dockKollection -> {
                    String path = dockKollection.getPath();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }

                    String urlPath = String.format("/%s", path);
                    String urlFullPath = String.format("/%s/", path);

                    return refererUrl.endsWith(urlPath) || refererUrl.contains(urlFullPath);
                })
                .findFirst().orElse(null);
        if (kollection == null) {
            if (log.isTraceEnabled()) {
                log.trace("No kollection found, return null");
            }
            return null;
        }

        if (log.isTraceEnabled()) {
            log.trace("Found session kollection, actor = {}, drama = {}, refererUrl = {}, roles = {}",
                    actorId, drama, refererUrl, kollection.getKollection().getId());
        }

        return kollection.getKollection().getId();
    }

    public String getOsid(String actorId) {
        //
        if (log.isTraceEnabled()) {
            log.trace("Get osid, actorId = {}", actorId);
        }

        CitizenKey citizenKey = null;
        if (StringUtils.hasText(actorId)) {
            citizenKey = getCitizenKey(actorId);
        }

        if (citizenKey == null) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot find osid, citizenKey = {}", citizenKey);
            }
            return null;
        }

        String osid = this.dockContext.getOsid(citizenKey);
        if (osid == null) {
            if (log.isTraceEnabled()) {
                log.trace("No osid found, return null");
            }
            return null;
        }

        return osid;
    }

    public String getUsid(String actorId) {
        //
        if (log.isTraceEnabled()) {
            log.trace("Get usid, actorId = {}", actorId);
        }

        CitizenKey citizenKey = null;
        if (StringUtils.hasText(actorId)) {
            citizenKey = getCitizenKey(actorId);
        }

        if (citizenKey == null) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot find usid, citizenKey = {}", citizenKey);
            }
            return null;
        }

        String usid = this.dockContext.getUsid(citizenKey);
        if (usid == null) {
            if (log.isTraceEnabled()) {
                log.trace("No usid found, return null");
            }
            return null;
        }

        return usid;
    }

    private CitizenKey getCitizenKey(String tenantId) {
        //
        TenantType tenantType = TenantKey.getTenantType(tenantId);

        if (tenantType == null) {
            throw new IllegalArgumentException("Cannot determine tenant type, tenantId = " + tenantId);
        }

        CitizenKey citizenKey = null;

        switch (tenantType) {
            case Actor:
                citizenKey = ActorKey.fromId(tenantId).genCitizenKey();
                break;
            case Audience:
                citizenKey = AudienceKey.fromId(tenantId).genCitizenKey();
                break;
            case Citizen:
                citizenKey = CitizenKey.fromId(tenantId);
                break;
            default:
                throw new IllegalArgumentException("Cannot determine tenant type, tenantId = " + tenantId);
        }

        if (log.isTraceEnabled()) {
            log.trace("Citizen key found, citizenKey = {}", citizenKey.getId());
        }

        return citizenKey;
    }

    private Dock getDock(CitizenKey citizenKey, String jti) {
        //
        return this.dockContext.getDock(citizenKey, jti);
    }
}
