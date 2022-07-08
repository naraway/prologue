/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter;

import io.naraway.accent.domain.trail.TrailInfo;

import javax.servlet.http.HttpServletRequest;

public interface StageRequestBuilder {
    //
    void buildRequest(HttpServletRequest request);

    void buildRequest(TrailInfo trailInfo);

    void clearRequest();
}
