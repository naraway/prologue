package io.naraway.prologue.util.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class URLValidator {
    //
    @SuppressWarnings("java:S5998")
    private static final String REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern pattern = Pattern.compile(REGEX);

    public static boolean validate(String url) {
        //
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    public static boolean isValid(String url) {
        //
        return validate(url);
    }
}