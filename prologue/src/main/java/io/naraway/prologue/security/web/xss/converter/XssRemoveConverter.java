package io.naraway.prologue.security.web.xss.converter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XssRemoveConverter implements XssConverter {
    //
    @Override
    @SuppressWarnings("java:S5857")
    public String convert(String value) {
        //
        String result = value;

        if (result == null) {
            return null;
        }

        result = result.replaceAll("\\<.*?\\>", "");
        result = result.replaceAll("eval\\((.*)\\)", "");
        result = result.replaceAll("[\\\"\\'][\\s]*javascript:(.*)[\\\"\\']", "\"\"");

        if (log.isTraceEnabled()) {
            log.trace("Xss remove converter: value = {}, result = {}", value, result);
        }
        return result;
    }
}
