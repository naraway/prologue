/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.security.rolekeeper;

import io.naraway.accent.domain.annotation.AuthorizedRole;
import io.naraway.accent.domain.message.DomainMessage;
import io.naraway.prologue.security.context.StageContextBuilder;
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
public class ResourceRoleEndPointHolder {
    //
    private static final String PUBLIC = "isPublic";
    private static final String ANONYMOUS = "anonymous";
    private static final String AUTHORIZED_ROLE_DEFAULT = "value";
    private static final String AUTHORIZED_ROLE_ROLES = "roles";
    private static final String FORBIDDEN_AUTHORIZED_ROLE = "*FORBIDDEN*";

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final Set<String> publicUrlSet;
    private final Set<String> anonymousUrlSet;
    private final Set<String> authorizeUrlSet;

    private final Map<String, Set<String>> resourceRoleMap;
    private final Map<String, Set<String>> messageRoleMap;

    public ResourceRoleEndPointHolder() {
        //
        this.publicUrlSet = new HashSet<>();
        this.anonymousUrlSet = new HashSet<>();
        this.authorizeUrlSet = new HashSet<>();

        this.resourceRoleMap = new HashMap<>();
        this.messageRoleMap = new HashMap<>();
    }

    @SuppressWarnings("java:S3776")
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        //
        ApplicationContext applicationContext = event.getApplicationContext();
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((mapping, method) -> {
            boolean isPublicMapping = parsePublicRole(method.getBeanType().getAnnotations())
                    || parsePublicRole(method.getMethod().getAnnotations())
                    || parsePublicRole(getMessageAnnotations(method.getMethod()));

            boolean isAnonymousMapping = parseRole(method.getBeanType().getAnnotations(), ANONYMOUS)
                    || parseRole(method.getMethod().getAnnotations(), ANONYMOUS)
                    || parseRole(getMessageAnnotations(method.getMethod()), ANONYMOUS);

            Set<String> resourceRole = new HashSet<>();
            resourceRole.addAll(getAnnotationRoles(method.getBeanType().getAnnotations()));
            resourceRole.addAll(getAnnotationRoles(method.getMethod().getAnnotations()));

            Set<String> messageRole = new HashSet<>(getAnnotationRoles(getMessageAnnotations(method.getMethod())));

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
                        log.trace("Authorized role is public, url = {}", pattern);
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

                    if (!resourceRole.isEmpty()) {
                        if (log.isTraceEnabled()) {
                            log.trace("Authorized resource role, url = {}, role = {}", pattern, resourceRole);
                        }
                        toExpression(pattern, requestMethods)
                                .forEach(expression -> resourceRoleMap.put(expression, resourceRole));
                        if (resourceRole.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
                            log.warn("No role specified in resource: url = {}", pattern);
                        }
                    }

                    if (!messageRole.isEmpty()) {
                        if (log.isTraceEnabled()) {
                            log.trace("Authorized message role, url = {}, role = {}", pattern, messageRole);
                        }
                        toExpression(pattern, requestMethods)
                                .forEach(expression -> messageRoleMap.put(expression, messageRole));
                        if (messageRole.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
                            log.warn("No role specified in message: url = {}", pattern);
                        }
                    }
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

    public boolean hasResourceRole(String url, HttpMethod method, Set<String> requestRoles) {
        //
        if (!CollectionUtils.isEmpty(requestRoles) &&
                requestRoles.size() == 1 &&
                requestRoles.contains(StageContextBuilder.ROLE_INTERNAL)) {
            return true;
        }

        Set<String> roles = findRoles(resourceRoleMap, url, method);

        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        if (roles.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
            return false;
        }

        return requestRoles.stream().anyMatch(requestRole -> {
            if (isFeaturePath(url) && requestRoles.contains(".")) {
                return StringUtils.hasText(requestRole) && roles.contains(getFeatureRole(url, requestRole));
            } else {
                return StringUtils.hasText(requestRole) && roles.contains(requestRole);
            }
        });
    }

    public boolean hasMessageRole(String url, HttpMethod method, Set<String> requestRoles) {
        //
        if (!CollectionUtils.isEmpty(requestRoles) &&
                requestRoles.size() == 1 &&
                requestRoles.contains(StageContextBuilder.ROLE_INTERNAL)) {
            return true;
        }

        Set<String> roles = findRoles(messageRoleMap, url, method);

        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        if (roles.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
            return false;
        }

        return requestRoles.stream().anyMatch(requestRole -> {
            if (isFeaturePath(url) && requestRoles.contains(".")) {
                return StringUtils.hasText(requestRole) && roles.contains(getFeatureRole(url, requestRole));
            } else {
                return StringUtils.hasText(requestRole) && roles.contains(requestRole);
            }
        });
    }

    private boolean isFeaturePath(String url) {
        //
        return url.contains("/feature/");
    }

    private String getFeatureRole(String url, String role) {
        //
        if (!StringUtils.hasText(url)) {
            return role;
        }

        List<String> paths = Arrays.asList(url.split("/"));
        String feature = paths.get(paths.indexOf("feature") + 1);

        if (!StringUtils.hasText(feature)) {
            return role;
        }

        return String.format("%s.%s", feature, role);
    }

    private Set<String> findRoles(Map<String, Set<String>> roleMap, String url, HttpMethod method) {
        //
        Set<String> roles = findMatchedRoles(roleMap, url, method);
        if (!CollectionUtils.isEmpty(roles)) {
            return roles;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return findMatchedRoles(roleMap, alternativeUrl, method);
        }

        return Collections.emptySet();
    }

    private boolean parsePublicRole(Annotation[] annotations) {
        //
        return Arrays.stream(annotations).anyMatch(annotation -> {
            if (annotation.annotationType().equals(AuthorizedRole.class)) {
                try {
                    Method annotationMethod = Arrays.stream(annotation.annotationType().getMethods())
                            .filter(method -> method.getName().equals(PUBLIC)).findFirst().orElse(null);
                    return Boolean.parseBoolean(annotationMethod.invoke(annotation, (Object[]) null).toString());
                } catch (Exception e) {
                    // do nothing
                }
                return false;
            }
            return false;
        });
    }

    private Collection<String> getAnnotationRoles(Annotation[] annotations) {
        //
        Set<String> roles = new HashSet<>();
        boolean matched = Arrays.stream(annotations).anyMatch(annotation -> {
            if (annotation.annotationType().equals(AuthorizedRole.class)) {
                roles.addAll(getAnnotationValues(annotation, AUTHORIZED_ROLE_DEFAULT));
                roles.addAll(getAnnotationValues(annotation, AUTHORIZED_ROLE_ROLES));
                return true;
            }
            return false;
        });

        // add forbidden role for empty tag
        if (matched && CollectionUtils.isEmpty(roles)) {
            roles.add(FORBIDDEN_AUTHORIZED_ROLE);
        }

        return matched ? roles : Collections.emptySet();
    }

    @SuppressWarnings("java:S5361")
    private Set<String> findMatchedRoles(Map<String, Set<String>> roleMap, String url, HttpMethod httpMethod) {
        //
        String stripUrl = stripUrl(url);
        String method = httpMethod.name();

        if (roleMap.containsKey(toMethodUrl(stripUrl)) || roleMap.containsKey(toMethodUrl(stripUrl, method))) {
            return roleMap.get(stripUrl);
        }

        String foundKey = roleMap.keySet().stream().filter(patternUrl -> {
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
        }).findFirst().orElse(null);

        if (foundKey == null) {
            return Collections.emptySet();
        }

        if (isFeaturePath(url)) {
            return roleMap.get(foundKey).stream().map(role -> getFeatureRole(url, role)).collect(Collectors.toSet());
        } else {
            return roleMap.get(foundKey);
        }
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

    private Collection<String> getAnnotationValues(Annotation annotation, String name) {
        //
        try {
            Method annotationMethod = Arrays.stream(annotation.annotationType().getMethods())
                    .filter(method -> method.getName().equals(name)).findFirst().orElse(null);
            if (annotationMethod == null) {
                return Collections.emptyList();
            } else {
                String[] values = (String[]) annotationMethod.invoke(annotation, (Object[]) null);
                return Arrays.asList(values);
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("exception while get annotation value", e);
            }
        }

        return Collections.emptyList();
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
