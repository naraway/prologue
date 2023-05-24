/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */
package io.naraway.prologue.security.exception;

import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public class NoSuchRoleException extends AccessDeniedException {
    //
    public NoSuchRoleException(List<String> currentRoles, String requiredRole) {
        //
        super("No such role. currentRoles = " + String.join(",", currentRoles) + ", requiredRole = " + requiredRole);
    }

    public NoSuchRoleException(Throwable cause) {
        //
        super(cause.getMessage());
    }
}
