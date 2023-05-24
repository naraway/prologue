package io.naraway.prologue.util.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailValidator {
    //
    @SuppressWarnings("java:S5998")
    private static final String REGEX =
            "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern pattern = Pattern.compile(REGEX);

    public static boolean validate(String email) {
        //
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isValid(String email) {
        //
        return validate(email);
    }
}