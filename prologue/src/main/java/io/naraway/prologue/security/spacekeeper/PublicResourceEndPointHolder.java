/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.spacekeeper;

import io.naraway.accent.domain.annotation.AuthorizedRole;
import io.naraway.accent.domain.message.DomainMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PublicResourceEndPointHolder {
    //
    private static final String PUBLIC = "isPublic";
    private static final String ANONYMOUS = "anonymous";

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final Set<String> publicUrlSet;
    private final Set<String> anonymousUrlSet;
    private final Set<String> authorizeUrlSet;

    public PublicResourceEndPointHolder() {
        //
        this.publicUrlSet = new HashSet<>();
        this.anonymousUrlSet = new HashSet<>();
        this.authorizeUrlSet = new HashSet<>();
    }

    @EventListener
    @SuppressWarnings("java:S3776")
    public void handleContextRefresh(ContextRefreshedEvent event) {
        //
        ApplicationContext applicationContext = event.getApplicationContext();
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((mapping, method) -> {
            boolean isPublicMapping = parseRole(method.getBeanType().getAnnotations(), PUBLIC)
                    || parseRole(method.getMethod().getAnnotations(), PUBLIC)
                    || parseRole(getMessageAnnotations(method.getMethod()), PUBLIC);

            boolean isAnonymousMapping = parseRole(method.getBeanType().getAnnotations(), ANONYMOUS)
                    || parseRole(method.getMethod().getAnnotations(), ANONYMOUS)
                    || parseRole(getMessageAnnotations(method.getMethod()), ANONYMOUS);

            Set<String> patterns = new HashSet<>();
            if (mapping.getPatternsCondition() != null) {
                patterns.addAll(mapping.getPatternsCondition().getPatterns());
            } else {
                patterns.addAll(mapping.getPathPatternsCondition().getPatterns().stream()
                        .map(PathPattern::getPatternString)
                        .collect(Collectors.toList()));
            }

            Set<RequestMethod> requestMethods = mapping.getMethodsCondition().getMethods();
            patterns.forEach(pattern -> {
                if (isPublicMapping) {
                    if (log.isTraceEnabled()) {
                        log.trace("{}.{}: {} is public",
                                method.getBeanType().getSimpleName(), method.getMethod().getName(), pattern);
                    }
                    publicUrlSet.addAll(toExpression(pattern, requestMethods));
                }
                if (isAnonymousMapping) {
                    if (log.isTraceEnabled()) {
                        log.trace("{}.{}: {} is anonymous",
                                method.getBeanType().getSimpleName(), method.getMethod().getName(), pattern);
                    }
                    anonymousUrlSet.addAll(toExpression(pattern, requestMethods));
                }

                List<String> expressions = toExpression(pattern, requestMethods);
                if (!isPublicMapping && !isAnonymousMapping) {
                    log.trace("{}.{}: {} is authorize",
                            method.getBeanType().getSimpleName(), method.getMethod().getName(), pattern);
                    expressions.forEach(authorizeUrlSet::add);
                }
            });
        });
    }

    public boolean isPublic(String url, HttpMethod method) {
        //
        if (isAuthorize(url, method)) {
            return false;
        }

        if (matches(publicUrlSet, url, method)) {
            return true;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return matches(publicUrlSet, alternativeUrl, method);
        }

        return false;
    }

    public boolean isAnonymous(String url, HttpMethod method) {
        //
        if (isAuthorize(url, method)) {
            return false;
        }

        if (matches(anonymousUrlSet, url, method)) {
            return true;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return matches(anonymousUrlSet, alternativeUrl, method);
        }

        return false;
    }

    private boolean isAuthorize(String url, HttpMethod method) {
        //
        if (matches(authorizeUrlSet, url, method)) {
            return true;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return matches(authorizeUrlSet, alternativeUrl, method);
        }

        return false;
    }

    @SuppressWarnings("java:S5361")
    private boolean matches(Set<String> patternUrls, String url, HttpMethod httpMethod) {
        //
        String stripUrl = stripUrl(url);
        String method = httpMethod.name();

        if (patternUrls.contains(toMethodUrl(stripUrl, method)) || patternUrls.contains(toMethodUrl(stripUrl))) {
            return true;
        }

        return patternUrls.stream().anyMatch(patternUrl -> {
            if (patternUrl.contains("*")) {
                boolean countMatched = countPath(stripUrl) == countPath(patternUrl);
                boolean matchedWithMethod = toMethodUrl(stripUrl, method)
                        .matches(patternUrl.replaceAll("/\\*", "/(.*)"));
                if (countMatched && matchedWithMethod) {
                    return true;
                }

                boolean matchedWithoutMethod = toMethodUrl(stripUrl)
                        .matches(patternUrl.replaceAll("/\\*", "/(.*)"));
                return countMatched && matchedWithoutMethod;
            }
            return false;
        });
    }

    private String stripUrl(String url) {
        //
        if (StringUtils.hasText(this.contextPath)) {
            return url.replace(this.contextPath, "");
        }

        return url;
    }

    private long countPath(String url) {
        //
        return url.chars().filter(c -> c == '/').count();
    }

    private Annotation[] getMessageAnnotations(Method method) {
        //
        List<Annotation> annotations = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        List<Parameter> domainMessageParameters = Arrays.stream(parameters)
                .filter(parameter -> DomainMessage.class.isAssignableFrom(parameter.getType()))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(domainMessageParameters)) {
            domainMessageParameters.stream().findFirst().ifPresent(parameter ->
                    annotations.addAll(Arrays.asList(parameter.getType().getAnnotations())));
        }

        return annotations.toArray(new Annotation[0]);
    }

    private boolean parseRole(Annotation[] annotations, String roleType) {
        //
        return Arrays.stream(annotations).anyMatch(annotation -> {
            if (annotation.annotationType().equals(AuthorizedRole.class)) {
                try {
                    Method annotationMethod = Arrays.stream(annotation.annotationType().getMethods())
                            .filter(method -> method.getName().equals(roleType)).findFirst().orElse(null);
                    return Boolean.parseBoolean(annotationMethod.invoke(annotation, (Object[]) null).toString());
                } catch (Exception e) {
                    // do nothing
                }
                return false;
            }
            return false;
        });
    }

    private List<String> toExpression(String url, Set<RequestMethod> methods) {
        //
        String[] parts = url.split("/");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("/");
            }
            builder.append(parts[i].startsWith("{") && parts[i].endsWith("}") ? "*" : parts[i]);
        }

        if (CollectionUtils.isEmpty(methods)) {
            return List.of(toMethodUrl(builder.toString()));
        }

        return methods.stream()
                .map(method -> toMethodUrl(builder.toString(), method.name()))
                .collect(Collectors.toList());
    }

    private String toMethodUrl(String url) {
        //
        return toMethodUrl(url, "REQUEST");
    }

    private String toMethodUrl(String url, String method) {
        //
        return String.format("%s:%s", method, url);
    }
}
