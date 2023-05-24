/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.shared.dock;

import io.naraway.accent.domain.rolemap.Kollectie;
import io.naraway.accent.domain.rolemap.KollectionRole;
import io.naraway.accent.domain.rolemap.KollectionVersionKey;
import io.naraway.accent.domain.tenant.*;
import io.naraway.accent.domain.type.IdName;
import io.naraway.accent.util.json.JsonSerializable;
import io.naraway.accent.util.json.JsonUtil;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dock implements JsonSerializable {
    //
    private IdName citizen;
    private IdName pavilion;
    private IdName defaultStage;
    private boolean defaultFirstForStage;
    private List<UserCineroom> cinerooms = new ArrayList<>();

    @Override
    public String toString() {
        //
        return this.toJson();
    }

    public static Dock fromJson(String json) {
        //
        return JsonUtil.fromJson(json, Dock.class);
    }

    public static Dock sample() {
        //
        return new Dock(
                IdName.of(CitizenKey.sample().getId(), "Steve Jobs"),
                IdName.of(PavilionKey.sample().getId(), "Nextree"),
                IdName.of(StageKey.sample().getId(), "Timeline"),
                true,
                List.of(UserCineroom.sample())
        );
    }

    public static void main(String[] args) {
        //
        System.out.println(sample().toPrettyJson());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCineroom {
        //
        private IdName audience;
        private IdName cineroom;
        private boolean base;
        private boolean active;
        private List<UserStage> stages;

        public static UserCineroom sample() {
            //
            return new UserCineroom(
                    IdName.of(AudienceKey.sample().getId(), "Steve"),
                    IdName.of(CineroomKey.sample().getId(), "Namoosori"),
                    true,
                    true,
                    List.of(UserStage.sample())
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserKollection {
        //
        private IdName kollection;
        private String path;
        private boolean active;
        private List<Kollectie> kollecties;
        private List<KollectionRole> kollectionRoles;

        public static UserKollection sample() {
            //
            return new UserKollection(
                    IdName.of(KollectionVersionKey.sample().getId(), "Class room"),
                    KollectionVersionKey.sample().genKollectionId().toLowerCase(),
                    true,
                    List.of(Kollectie.sample()),
                    List.of(KollectionRole.sample())
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStage {
        //
        private IdName actor;
        private IdName stage;
        private boolean base;
        private boolean active;
        private List<UserKollection> kollections;

        public static UserStage sample() {
            //
            return new UserStage(
                    IdName.of(ActorKey.sample().getId(), "Steve"),
                    IdName.of(StageKey.sample().getId(), "MSA Course"),
                    true,
                    true,
                    List.of(UserKollection.sample())
            );
        }
    }
}
