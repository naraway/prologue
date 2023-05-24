/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.Collections;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@EnableConfigurationProperties(SpringDocProperties.class)
@RequiredArgsConstructor
public class SpringDocAutoConfiguration {
    //
    private final SpringDocProperties properties;

    @Bean
    public OpenAPI openAPI() {
        //
        Info info = new Info()
                .title(String.format("%s API", this.properties.getTitle().toUpperCase()))
                .version(this.properties.getVersion())
                .contact(new Contact()
                        .name(this.properties.getInfo().getName())
                        .email(this.properties.getInfo().getEmail())
                        .url(this.properties.getInfo().getUrl()));

        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);

        SecurityRequirement addSecurityItem = new SecurityRequirement();
        addSecurityItem.addList("JWT");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("JWT", bearerAuth))
                .addSecurityItem(addSecurityItem)
                .info(info);
    }

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        //
        String url = this.properties.getSwaggerUi().getUrl();

        if (StringUtils.hasText(url)) {
            String proxyContextPath = url.substring(0, url.indexOf("/v3/api-docs"));
            return openApi -> openApi.servers(Collections.singletonList(
                    new Server().url(proxyContextPath).description("service")));
        }

        return openApi -> openApi.servers(Collections.singletonList(
                new Server().url("").description("localhost")));
    }
}
