package io.naraway.prologue.util.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NumericValidator {
    //
    @SuppressWarnings("java:S5998")
    private static final String REGEX = "[+-]?\\d*(\\.\\d+)?";
    private static final Pattern pattern = Pattern.compile(REGEX);

    public static boolean validate(String numeric) {
        //
        Matcher matcher = pattern.matcher(numeric);
        return matcher.matches();
    }

    public static boolean isValid(String numeric) {
        //
        return validate(numeric);
    }
}
