package io.naraway.drama.prologue.spacekeeper.support;

import java.util.List;

public class NoSuchRoleException extends SecurityException {
    //
    public NoSuchRoleException() {
        //
        super();
    }

    public NoSuchRoleException(List<String> currentRoles, String requiredRole) {
        //
        super("No such role. currentRoles = " + String.join(",", currentRoles) + ", requiredRole = " + requiredRole);
    }

    public NoSuchRoleException(Throwable cause) {
        //
        super(cause);
    }
}
