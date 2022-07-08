/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.security;

import io.naraway.accent.domain.ddd.AuthorizedRole;
import io.naraway.accent.domain.trail.TrailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PublicResourceEndPointHolder {
    //
    private static final String PUBLIC = "isPublic";
    private static final String ANONYMOUS = "anonymous";

    private final Set<String> publicUrlSet;
    private final Set<String> anonymousUrlSet;

    public PublicResourceEndPointHolder() {
        //
        this.publicUrlSet = new HashSet<>();
        this.anonymousUrlSet = new HashSet<>();
    }

    @EventListener
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

            patterns.forEach(pattern -> {
                if (isPublicMapping) {
                    log.trace("{}.{}: {} is public",
                            method.getBeanType().getSimpleName(), method.getMethod().getName(), pattern);
                    publicUrlSet.add(toExpression(pattern));
                }
                if (isAnonymousMapping) {
                    log.trace("{}.{}: {} is anonymous",
                            method.getBeanType().getSimpleName(), method.getMethod().getName(), pattern);
                    anonymousUrlSet.add(toExpression(pattern));
                }
            });
        });
    }

    public boolean isPublic(String url) {
        //
        if (matches(publicUrlSet, url)) {
            return true;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return matches(publicUrlSet, alternativeUrl);
        }

        return false;
    }

    public boolean isAnonymous(String url) {
        //
        if (matches(anonymousUrlSet, url)) {
            return true;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return matches(anonymousUrlSet, alternativeUrl);
        }

        return false;
    }

    @SuppressWarnings("java:S5361")
    private boolean matches(Set<String> patternUrls, String url) {
        //
        if (patternUrls.contains(url)) {
            return true;
        }

        return patternUrls.stream().anyMatch(patternUrl -> {
            if (patternUrl.contains("*")) {
                return url.matches(patternUrl.replaceAll("/\\*", "/(.*)"));
            }
            return false;
        });
    }

    private Annotation[] getMessageAnnotations(Method method) {
        //
        List<Annotation> annotations = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        List<Parameter> domainMessageParameters = Arrays.stream(parameters)
                .filter(parameter -> TrailMessage.class.isAssignableFrom(parameter.getType()))
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

    private String toExpression(String url) {
        //
        String[] parts = url.split("/");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("/");
            }
            builder.append(parts[i].startsWith("{") && parts[i].endsWith("}") ? "*" : parts[i]);
        }

        return builder.toString();
    }
}
