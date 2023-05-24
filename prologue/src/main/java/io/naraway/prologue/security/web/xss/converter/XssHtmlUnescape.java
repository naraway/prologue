package io.naraway.prologue.security.web.xss.converter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.HtmlUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XssHtmlUnescape {
    //
    public static String unescape(String value) {
        //
        String result = value;

        if (result == null) {
            return null;
        }

        result = HtmlUtils.htmlUnescape(result);

        return result;
    }
}
