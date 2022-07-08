/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter;

import io.naraway.accent.util.json.JsonSerializable;

public interface StageRequest extends JsonSerializable {
    //
    boolean hasStageAuthority();

    boolean hasRole(String role);
}
