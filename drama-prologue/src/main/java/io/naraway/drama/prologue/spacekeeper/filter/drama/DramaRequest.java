/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter.drama;

import io.naraway.accent.domain.key.stage.ActorKey;
import io.naraway.accent.domain.key.tenant.CineroomKey;
import io.naraway.accent.domain.key.tenant.CitizenKey;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class DramaRequest implements StageRequest {
    //
    public static final String ACTOR_ID_NAME = "actorId";
    public static final String ROLE_NAME = "roles";

    private String actorId;
    private String loginId;
    private String displayName;
    private String citizenId;
    private List<String> cineroomIds;
    private List<String> roles;

    public static DramaRequest sample() {
        //
        String actorId = ActorKey.sample().getId();
        String loginId = "s.jobs@domain.io";
        String displayName = "Steve Jobs";
        String citizenId = CitizenKey.sample().getId();
        List<String> cineroomIds = Arrays.asList(CineroomKey.sample().getId());
        List<String> roles = Arrays.asList("admin", "manager", "user");

        return new DramaRequest(
                actorId,
                loginId,
                displayName,
                citizenId,
                cineroomIds,
                roles
        );
    }

    @Override
    public boolean hasStageAuthority() {
        //
        String currentCineroomId = null;
        if (actorId != null && !CollectionUtils.isEmpty(cineroomIds)) {
            currentCineroomId = ActorKey.fromId(actorId).genCineroomKey().getId();
        }

        if (!CollectionUtils.isEmpty(cineroomIds)) {
            return cineroomIds.contains(currentCineroomId);
        }

        log.debug("Authorization was failed, actorId = {}, currentCineroomId = {}", actorId, currentCineroomId);

        return false;
    }

    @Override
    public boolean hasRole(String role) {
        //
        return this.roles.contains(role);
    }

    @Override
    public String toString() {
        //
        return toJson();
    }
}
