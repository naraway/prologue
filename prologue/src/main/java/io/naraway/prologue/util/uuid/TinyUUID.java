package io.naraway.prologue.util.uuid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Tiny 64 UUID util
 *
 * @deprecated This descriptor is no longer acceptable in next version.
 * Use {@link TinyUUID on accent module} instead.
 */
@Deprecated(since = "4.1.0", forRemoval = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S1133")
public class TinyUUID {
    //
    private static final char[] DIGITS66 = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '-', '_', '=', '~'
    };

    public static String random() {
        //
        UUID uuid = UUID.randomUUID();
        return toIDString(uuid.getMostSignificantBits()) + toIDString(uuid.getLeastSignificantBits());
    }

    public static String random(String prefix) {
        //
        return String.format("%s_%s", prefix, random());
    }

    public static String random(String prefix, String separator) {
        //
        return String.format("%s%s%s", prefix, separator, random());
    }

    private static String toIDString(long i) {
        //
        char[] buffer = new char[32];
        int z = 64;
        int cp = 32;
        long b = z - 1L;

        do {
            buffer[--cp] = DIGITS66[(int) (i & b)];
            i >>>= 6;
        } while (i != 0);

        return new String(buffer, cp, (32 - cp));
    }

    public static void main(String[] args) {
        //
        System.out.println(random());
        System.out.println(random("usid"));
        System.out.println(random("usid", "_D"));
    }
}
