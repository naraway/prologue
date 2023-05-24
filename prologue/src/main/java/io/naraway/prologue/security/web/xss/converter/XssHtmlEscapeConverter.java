package io.naraway.prologue.security.web.xss.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.HtmlUtils;

@Slf4j
public class XssHtmlEscapeConverter implements XssConverter {
    //
    @Override
    public String convert(String value) {
        //
        String result = value;

        if (result == null) {
            return null;
        }

        result = HtmlUtils.htmlEscape(result);

        if (log.isTraceEnabled()) {
            log.trace("Xss escape converter: value = {}, result = {}", value, result);
        }

        return result;
    }
}
