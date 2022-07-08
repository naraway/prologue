package io.naraway.drama.prologue.spacekeeper.support;

public class NoIdenticalPersonException extends SecurityException {
    //
    public NoIdenticalPersonException() {
        //
        super();
    }

    public NoIdenticalPersonException(String currentId, String requestId) {
        //
        super("No Identical person. currentId = " + currentId + ", requestId = " + requestId);
    }

    public NoIdenticalPersonException(Throwable cause) {
        //
        super(cause);
    }
}
