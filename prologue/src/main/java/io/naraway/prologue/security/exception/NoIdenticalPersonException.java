/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */
package io.naraway.prologue.security.exception;

import org.springframework.security.access.AccessDeniedException;

public class NoIdenticalPersonException extends AccessDeniedException {
    //
    public NoIdenticalPersonException(String currentId, String requestId) {
        //
        super("No Identical person. currentId = " + currentId + ", requestId = " + requestId);
    }

    public NoIdenticalPersonException(Throwable cause) {
        //
        super(cause.getMessage());
    }
}
