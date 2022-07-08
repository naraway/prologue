/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.support;

import io.naraway.drama.prologue.spacekeeper.filter.StageRequestContext;
import io.naraway.drama.prologue.spacekeeper.filter.drama.DramaRequest;

public class DramaRequestContext {
    //
    private DramaRequestContext() {
        //
    }

    public static DramaRequest current() {
        //
        return (DramaRequest) StageRequestContext.get();
    }

    public static void setSampleContext() {
        //
        StageRequestContext.set(DramaRequest.sample());
    }

    public static void setSampleContext(DramaRequest sampleReuqest) {
        //
        StageRequestContext.set(sampleReuqest);
    }
}
