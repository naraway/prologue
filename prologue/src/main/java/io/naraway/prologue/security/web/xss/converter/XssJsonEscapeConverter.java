package io.naraway.prologue.security.web.xss.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class XssJsonEscapeConverter implements XssConverter {
    //
    private static final List<String> escapeLetters = Arrays.asList("<", ">");

    @Override
    public String convert(String value) {
        //
        String result = value;

        if (result == null) {
            return null;
        }

        for (String letter : escapeLetters) {
            result = result.replaceAll(letter, HtmlUtils.htmlEscape(letter));
        }

        if (log.isTraceEnabled()) {
            log.trace("Xss escape converter: value = {}, result = {}", value, result);
        }
        return result;
    }
}
