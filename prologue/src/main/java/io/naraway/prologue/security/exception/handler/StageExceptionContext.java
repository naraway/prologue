/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.exception.handler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StageExceptionContext {
    //
    private static final ThreadLocal<String> context = new ThreadLocal<>();

    public static String get() {
        //
        return context.get();
    }

    public static void set(String exceptionCode) {
        //
        context.set(exceptionCode);
    }

    public static void clear() {
        //
        context.remove();
    }

    public static boolean exists() {
        //
        return context.get() != null;
    }
}