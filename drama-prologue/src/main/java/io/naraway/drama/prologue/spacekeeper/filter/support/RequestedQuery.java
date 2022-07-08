/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter.support;

import io.naraway.accent.domain.trail.AbstractQuery;
import io.naraway.accent.domain.trail.TrailMessageType;

@SuppressWarnings("java:S3740")
public class RequestedQuery extends AbstractQuery {
    //
    public RequestedQuery() {
        //
        super(TrailMessageType.QueryRequest);
    }
}
