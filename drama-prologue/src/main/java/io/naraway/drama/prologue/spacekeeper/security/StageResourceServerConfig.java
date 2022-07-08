/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.security;

import io.naraway.drama.prologue.spacekeeper.filter.StageAuthenticationFilter;
import io.naraway.drama.prologue.spacekeeper.filter.StageExceptionResolver;
import io.naraway.drama.prologue.spacekeeper.filter.StageRequestBuilder;
import io.naraway.drama.prologue.spacekeeper.filter.drama.DramaRequest;
import io.naraway.drama.prologue.spacekeeper.filter.support.StageAuthenticationMockFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("java:S1874")
@Configuration
@EnableResourceServer
@ConditionalOnWebApplication
@ConditionalOnMissingClass("io.naraway.checkpoint.config.OAuth2ResourceServerConfig")
@RequiredArgsConstructor
@Slf4j
public class StageResourceServerConfig extends ResourceServerConfigurerAdapter {
    private final StageRequestBuilder requestBuilder;
    private final StageAuthenticationEntryPoint entryPoint;
    private final PublicResourceEndPointHolder endPointHolder;
    //
    @Value("${nara.signing-key:default}")
    private String jwtSigningKey;
    @Value("${spring.profiles.active:default}")
    private List<String> activeProfiles;
    @Value("${nara.test-profiles:default,k8s-test}")
    private List<String> testProfiles;
    // NOTE: testing drama request
    @Value("${nara.drama.default.actorId:1@1:1:1:1-1}")
    private String defaultActorId;
    @Value("${nara.drama.default.loginId:user@company.io}")
    private String defaultLoginId;
    @Value("${nara.drama.default.displayName:User}")
    private String defaultDisplayName;
    @Value("${nara.drama.default.citizenId:1@1:1:1:1}")
    private String defaultCitizenId;
    @Value("${nara.drama.default.cineroomIds:1:1:1:1,1:1:1:2,1:1:1:3}")
    private List<String> defaultCineroomIds;
    @Value("${nara.drama.default.roles:user}")
    private List<String> defaultRoles;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        //
        if (!testProfiles.isEmpty() && testProfiles.stream()
                .anyMatch(authIgnoreProfile -> activeProfiles.contains(authIgnoreProfile))) {
            http.authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .and()
                    .addFilterBefore(new StageAuthenticationMockFilter(
                                    requestBuilder, endPointHolder,
                                    defaultRequest()),
                            BasicAuthenticationFilter.class)
                    .addFilterBefore(new StageExceptionResolver(), StageAuthenticationMockFilter.class);

            log.info("Space keeper authentication was disabled by test profiles = {}", activeProfiles);
        } else {
            http.authorizeRequests()
                    .requestMatchers(new PublicResourceMatcher(endPointHolder)).permitAll()
                    .and()
                    .authorizeRequests()
                    .antMatchers(
                            "/swagger-ui.html**",
                            "/swagger-ui/**",
                            "/swagger-resources/**",
                            "/actuator/**",
                            "/v2/api-docs/**",
                            "/v3/api-docs/**",
                            "/webjars/**").permitAll()
                    .and()
                    .authorizeRequests().anyRequest().authenticated()
                    .and()
                    .addFilterAfter(new StageAuthenticationFilter(requestBuilder, endPointHolder, activeProfiles, testProfiles),
                            AnonymousAuthenticationFilter.class)
                    .addFilterBefore(new StageExceptionResolver(), StageAuthenticationFilter.class);

            log.info("Space keeper authentication was activated");
            log.info("Registered filter list = {}", Arrays.asList(
                    StageAuthenticationFilter.class.getSimpleName()));
        }
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer config) {
        //
        config.tokenServices(tokenServices());
        config.authenticationEntryPoint(entryPoint);
    }

    @Bean
    public TokenStore tokenStore() {
        //
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Primary
    @Bean
    public DefaultTokenServices tokenServices() {
        //
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setSupportRefreshToken(true);

        return defaultTokenServices;
    }

    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        //
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(jwtSigningKey);

        return converter;
    }

    public DramaRequest defaultRequest() {
        //
        return new DramaRequest(
                this.defaultActorId,
                this.defaultLoginId,
                this.defaultDisplayName,
                this.defaultCitizenId,
                this.defaultCineroomIds,
                this.defaultRoles);
    }
}
