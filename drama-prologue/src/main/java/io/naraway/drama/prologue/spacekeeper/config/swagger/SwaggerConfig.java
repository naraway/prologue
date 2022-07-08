/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.config.swagger;

import com.fasterxml.classmate.TypeResolver;
import io.naraway.accent.domain.key.stage.ActorKey;
import io.naraway.accent.domain.trail.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebMvc
public class SwaggerConfig {
    //
    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active}")
    private List<String> activeProfiles;

    @Value("${nara.swagger.basePath:/api/${spring.application.name}}")
    private String basePath;

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        //
        return new BeanPostProcessor() {
            //
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                //
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                //
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings({"unchecked", "java:S3011"})
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                //
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    if (field == null) {
                        throw new NullPointerException();
                    }
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Bean
    public Docket api() {
        //
        String prefix = activeProfiles.stream().anyMatch(activeProfile -> activeProfile.contains("k8s")) ? basePath : "";
        TypeResolver typeResolver = new TypeResolver();

        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build().apiInfo(getApiInfo())
                // simple swagger model
                .genericModelSubstitutes(QueryResponse.class)
                .ignoredParameterTypes(
                        ActorKey.class,
                        TrailMessageType.class,
                        CommandResponse.class,
                        FailureMessage.class,
                        TrailInfo.class,
                        QueryResponse.class)
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(ActorKey.class), typeResolver.resolve(Void.class)))
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(TrailMessageType.class), typeResolver.resolve(Void.class)))
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(CommandResponse.class), typeResolver.resolve(Void.class)))
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(FailureMessage.class), typeResolver.resolve(Void.class)))
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(TrailInfo.class), typeResolver.resolve(Void.class)))
                .alternateTypeRules(AlternateTypeRules.newRule(
                        typeResolver.resolve(QueryResponse.class), typeResolver.resolve(Void.class)))
                // put mapping for k8s
                .pathMapping(prefix);
    }

    private ApiInfo getApiInfo() {
        //
        String name = applicationName.substring(0, 1).toUpperCase() + applicationName.substring(1);

        return new ApiInfo(
                name + " Api Documentation",
                name + " Api Documentation",
                "3.0.0",
                "urn:tos",
                new Contact("Nara Way", "http://naraway.io", "naraway@nextree.io"),
                "private",
                "http://naraway.io/licenses",
                new ArrayList<>());
    }
}
