package io.naraway.prologue.security.web.xss.converter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XssJsonUnescape {
    //
    private static final List<String> unescapeLetters = Arrays.asList("<", ">");

    public static String unescape(String value) {
        //
        String result = value;

        if (result == null) {
            return null;
        }

        for (String letter : unescapeLetters) {
            result = result.replaceAll(HtmlUtils.htmlEscape(letter), letter);
        }

        return result;
    }
}
