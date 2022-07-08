/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter;

public class StageRequestContext {
    private static final ThreadLocal<StageRequest> threadLocal = new ThreadLocal<>();

    //
    private StageRequestContext() {
        //
    }

    public static void clear() {
        //
        threadLocal.remove();
    }

    public static StageRequest get() {
        //
        return threadLocal.get();
    }

    public static void set(StageRequest stageRequest) {
        //
        threadLocal.set(stageRequest);
    }
}
