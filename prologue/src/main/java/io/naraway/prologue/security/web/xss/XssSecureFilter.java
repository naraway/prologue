package io.naraway.prologue.security.web.xss;

import io.naraway.prologue.autoconfigure.PrologueProperties;
import io.naraway.prologue.security.web.xss.converter.XssConverter;
import io.naraway.prologue.security.web.xss.converter.XssHtmlEscapeConverter;
import io.naraway.prologue.security.web.xss.converter.XssJsonEscapeConverter;
import io.naraway.prologue.security.web.xss.converter.XssRemoveConverter;
import io.naraway.prologue.security.web.xss.helper.XssRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class XssSecureFilter implements Filter {
    //
    private final boolean enabled;
    private final List<String> excludeUrls;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private XssConverter xssConverter;
    private XssConverter jsonXssConverter;

    public XssSecureFilter(PrologueProperties properties) {
        //
        this.enabled = properties.getMethodSecure().isEnabled();
        this.excludeUrls = CollectionUtils.isEmpty(properties.getXssSecure().getExcludeUrls())
                ? Collections.emptyList() : properties.getXssSecure().getExcludeUrls();

        switch (properties.getXssSecure().getConverter()) {
            case REMOVE:
                this.xssConverter = new XssRemoveConverter();
                this.jsonXssConverter = new XssRemoveConverter();
                break;
            case ESCAPE:
                this.xssConverter = new XssHtmlEscapeConverter();
                this.jsonXssConverter = new XssJsonEscapeConverter();
                break;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        String requestedUri = servletRequest.getRequestURI();
        String contentType = request.getContentType();

        if (this.enabled && !excluded(requestedUri)) {
            if (MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
                XssRequestWrapper wrappedRequest = new XssRequestWrapper(servletRequest, this.jsonXssConverter);
                String body = IOUtils.toString(wrappedRequest.getReader());
                if (StringUtils.hasText(body)) {
                    String securedBody = this.jsonXssConverter.convert(body);
                    wrappedRequest.resetInputStream(securedBody.getBytes());
                }
                chain.doFilter(wrappedRequest, response);
            } else if (isFormContentType(contentType)) {
                XssRequestWrapper wrappedRequest = new XssRequestWrapper(servletRequest, this.xssConverter);
                chain.doFilter(wrappedRequest, response);
            } else if (isXmlOrHtmlContentType(contentType)) {
                chain.doFilter(request, response);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isXmlOrHtmlContentType(String contentType) {
        //
        if (!StringUtils.hasText(contentType)) {
            return false;
        } else if (contentType.startsWith("application") &&
                (contentType.contains("xml") || contentType.contains("html"))) {
            return true;
        } else return contentType.startsWith("text") &&
                (contentType.contains("xml") || contentType.contains("html"));
    }

    private boolean isFormContentType(String contentType) {
        //
        if (!StringUtils.hasText(contentType)) {
            return false;
        } else return contentType.startsWith("application") && contentType.contains("-form-");
    }

    private boolean excluded(String url) {
        //
        if (log.isTraceEnabled()) {
            log.trace("Check exclude uri = {}", url);
        }
        return excludeUrls.stream().anyMatch(excludeUrl -> antPathMatcher.match(excludeUrl, url));
    }
}
