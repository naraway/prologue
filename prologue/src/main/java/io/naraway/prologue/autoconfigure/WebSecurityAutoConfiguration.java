package io.naraway.prologue.autoconfigure;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.naraway.prologue.security.auth.JwtAuthenticationFilter;
import io.naraway.prologue.security.auth.StageAccessDeniedHandler;
import io.naraway.prologue.security.auth.StageAuthenticationEntryPoint;
import io.naraway.prologue.security.auth.jwt.JwkTools;
import io.naraway.prologue.security.auth.jwt.JwtSupport;
import io.naraway.prologue.security.context.StageContextBuilder;
import io.naraway.prologue.security.context.StageContextLoopbackFilter;
import io.naraway.prologue.security.exception.handler.StageExceptionFilter;
import io.naraway.prologue.security.rolekeeper.ResourceRoleEndPointHolder;
import io.naraway.prologue.security.rolekeeper.RoleAuthenticationFilter;
import io.naraway.prologue.security.spacekeeper.PublicResourceEndPointHolder;
import io.naraway.prologue.security.spacekeeper.PublicResourceMatcher;
import io.naraway.prologue.security.spacekeeper.SpaceAuthenticationFilter;
import io.naraway.prologue.security.web.method.MethodSecureFilter;
import io.naraway.prologue.security.web.trace.TraceFilter;
import io.naraway.prologue.security.web.xss.XssSecureFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@AutoConfiguration(after = PrologueAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@RequiredArgsConstructor
public class WebSecurityAutoConfiguration {
    //
    private final PrologueProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtSupport jwtSupport,
            StageContextBuilder stageContextBuilder,
            @Autowired(required = false) PublicResourceEndPointHolder publicResourceEndPointHolder,
            @Autowired(required = false) ResourceRoleEndPointHolder resourceRoleEndPointHolder) throws Exception {
        //
        StageExceptionFilter stageExceptionFilter = new StageExceptionFilter();
        XssSecureFilter xssSecureFilter = new XssSecureFilter(this.properties);
        MethodSecureFilter methodSecureFilter = new MethodSecureFilter(this.properties);
        TraceFilter traceFilter = new TraceFilter();

        if (this.properties.isEnabled()) {
            StageAuthenticationEntryPoint stageAuthenticationEntryPoint = new StageAuthenticationEntryPoint();
            StageAccessDeniedHandler stageAccessDeniedHandler = new StageAccessDeniedHandler();

            JwtAuthenticationFilter tokenAuthenticationFilter = new JwtAuthenticationFilter(
                    jwtSupport, publicResourceEndPointHolder, getIgnoreUrls());
            SpaceAuthenticationFilter spaceAuthenticationFilter = new SpaceAuthenticationFilter(
                    this.properties, stageContextBuilder, publicResourceEndPointHolder);
            RoleAuthenticationFilter roleAuthenticationFilter = new RoleAuthenticationFilter(
                    resourceRoleEndPointHolder);

            http.cors().and().csrf().disable()
                    .headers().frameOptions().sameOrigin().and()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .exceptionHandling()
                    .authenticationEntryPoint(stageAuthenticationEntryPoint)
                    .accessDeniedHandler(stageAccessDeniedHandler).and()
                    .authorizeRequests().antMatchers(getIgnoreUrls()).permitAll().and()
                    .authorizeRequests().requestMatchers(
                            new PublicResourceMatcher(publicResourceEndPointHolder)).permitAll().and()
                    .authorizeRequests().anyRequest().authenticated().and()
                    .addFilterBefore(stageExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(spaceAuthenticationFilter, AnonymousAuthenticationFilter.class)
                    .addFilterAfter(roleAuthenticationFilter, SpaceAuthenticationFilter.class)
                    .addFilterBefore(xssSecureFilter, SpaceAuthenticationFilter.class)
                    .addFilterBefore(methodSecureFilter, XssSecureFilter.class)
                    .addFilterBefore(traceFilter, MethodSecureFilter.class);

            log.info("Prologue security filter chain was enabled");
        } else {
            StageContextLoopbackFilter stageContextLoopbackFilter =
                    new StageContextLoopbackFilter(this.properties, stageContextBuilder);

            http.cors().and().csrf().disable()
                    .headers().frameOptions().sameOrigin().and()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .exceptionHandling().and()
                    .authorizeRequests().antMatchers("/**").permitAll().and()
                    .addFilterBefore(stageExceptionFilter, BasicAuthenticationFilter.class)
                    .addFilterBefore(stageContextLoopbackFilter, BasicAuthenticationFilter.class)
                    .addFilterBefore(xssSecureFilter, StageContextLoopbackFilter.class)
                    .addFilterBefore(methodSecureFilter, XssSecureFilter.class)
                    .addFilterBefore(traceFilter, MethodSecureFilter.class);

            log.info("Prologue security filter chain was disabled");
        }

        log.info("Registered filter list = {}", List.of(SpaceAuthenticationFilter.class.getSimpleName()));
        return http.build();
    }

    private String[] getIgnoreUrls() {
        //
        List<String> ignoreUrls = new ArrayList<>(Arrays.asList(
                "/images/**",
                "/js/**",
                "/webjars/**",
                "/actuator/**",
                "/swagger-ui.html**",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/v2/api-docs/**",
                "/v3/api-docs/**"
        ));

        if (!CollectionUtils.isEmpty(this.properties.getPermitAllAntMatchers())) {
            ignoreUrls.addAll(this.properties.getPermitAllAntMatchers());
        }

        return ignoreUrls.toArray(new String[0]);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtSupport jwtSupport(JwtDecoder jwtDecoder) {
        //
        return new JwtSupport(jwtDecoder);
    }

    @Bean
    @ConditionalOnMissingBean
    public StageContextBuilder stageContextBuilder(JwtSupport jwtSupport) {
        //
        return new StageContextBuilder(jwtSupport);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.enabled", havingValue = "true", matchIfMissing = true)
    public PublicResourceEndPointHolder publicResourceEndPointHolder() {
        //
        return new PublicResourceEndPointHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nara.prologue.enabled", havingValue = "true", matchIfMissing = true)
    public ResourceRoleEndPointHolder resourceRoleEndPointHolder() {
        //
        return new ResourceRoleEndPointHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> jwkSource() {
        //
        String publicKey = this.properties.getJwtSigningPublicKey();
        String privateKey = this.properties.getJwtSigningPrivateKey();
        String keyId = this.properties.getJwtSigningKeyId();
        JWK jwk = StringUtils.hasText(privateKey) && StringUtils.hasText(keyId)
                ? JwkTools.rsaKey(publicKey, privateKey, keyId)
                : JwkTools.rsaKey();
        JWKSet jwkSet = new JWKSet(jwk);

        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        //
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
