/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.rolekeeper.filter;

import io.naraway.accent.domain.ddd.AuthorizedRole;
import io.naraway.accent.domain.trail.TrailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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
public class RoleResourceEndPointHolder {
    //
    private static final String PUBLIC = "isPublic";
    private static final String AUTHORIZED_ROLE_DEFAULT = "value";
    private static final String AUTHORIZED_ROLE_ROLES = "roles";
    private static final String FORBIDDEN_AUTHORIZED_ROLE = "*FORBIDDEN*";

    private final Set<String> publicUrlSet;
    private final Map<String, Set<String>> resourceRoleMap;
    private final Map<String, Set<String>> messageRoleMap;

    public RoleResourceEndPointHolder() {
        //
        this.publicUrlSet = new HashSet<>();
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

            patterns.forEach(pattern -> {
                if (isPublicMapping) {
                    log.trace("Authorized role is public, url = {}", pattern);
                    publicUrlSet.add(toExpression(pattern));
                } else {
                    if (!resourceRole.isEmpty()) {
                        log.trace("Authorized resource role, url = {}, role = {}", toExpression(pattern), resourceRole);
                        resourceRoleMap.put(toExpression(pattern), resourceRole);
                        if (messageRole.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
                            log.warn("No role specified in resource: url = {}", toExpression(pattern));
                        }
                    }

                    if (!messageRole.isEmpty()) {
                        log.trace("Authorized message role, url = {}, role = {}", toExpression(pattern), messageRole);
                        messageRoleMap.put(toExpression(pattern), messageRole);
                        if (messageRole.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
                            log.warn("No role specified in message: url = {}", toExpression(pattern));
                        }
                    }
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

    public boolean hasResourceRole(String url, Set<String> requestRoles) {
        //
        Set<String> roles = findRole(resourceRoleMap, url);

        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        if (roles.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
            return false;
        }

        return requestRoles.stream().anyMatch(requestRole
                -> StringUtils.hasText(requestRole) && roles.contains(requestRole));
    }

    public boolean hasMessageRole(String url, Set<String> requestRoles) {
        //
        Set<String> roles = findRole(messageRoleMap, url);

        if (CollectionUtils.isEmpty(roles)) {
            return true;
        }

        if (roles.contains(FORBIDDEN_AUTHORIZED_ROLE)) {
            return false;
        }

        return requestRoles.stream().anyMatch(requestRole
                -> StringUtils.hasText(requestRole) && roles.contains(requestRole));
    }

    private Set<String> findRole(Map<String, Set<String>> roleMap, String url) {
        //
        Set<String> role = findMatchedRole(roleMap, url);
        if (!CollectionUtils.isEmpty(role)) {
            return role;
        }

        if (url.endsWith("/")) {
            String alternativeUrl = url.substring(0, url.length() - 1);
            return findMatchedRole(roleMap, alternativeUrl);
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
    private Set<String> findMatchedRole(Map<String, Set<String>> roleMap, String url) {
        //
        if (roleMap.containsKey(url)) {
            return roleMap.get(url);
        }

        String foundKey = roleMap.keySet().stream().filter(patternUrl -> {
            if (patternUrl.contains("*")) {
                return url.matches(patternUrl.replaceAll("/\\*", "/(.*)"));
            }
            return false;
        }).findFirst().orElse(null);

        if (foundKey == null) {
            return Collections.emptySet();
        }

        return roleMap.get(foundKey);
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
            log.trace("exception while get annotation value", e);
        }

        return Collections.emptyList();
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
