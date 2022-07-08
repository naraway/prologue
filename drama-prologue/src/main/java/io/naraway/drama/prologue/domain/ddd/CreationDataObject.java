/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.domain.ddd;

import io.naraway.accent.domain.key.stage.ActorKey;
import io.naraway.accent.util.json.JsonSerializable;
import io.naraway.drama.prologue.spacekeeper.support.DramaRequestContext;

import java.util.UUID;

public abstract class CreationDataObject implements JsonSerializable {
    //
    protected ActorKey requesterKey;

    protected CreationDataObject() {
        //
        if (DramaRequestContext.current() != null) {
            requesterKey = ActorKey.fromId(DramaRequestContext.current().getActorId());
        }
    }

    public String genId() {
        //
        return UUID.randomUUID().toString();
    }

    public ActorKey getRequesterKey() {
        //
        return requesterKey;
    }
}
