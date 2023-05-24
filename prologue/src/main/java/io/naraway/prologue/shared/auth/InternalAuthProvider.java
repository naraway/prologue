package io.naraway.prologue.shared.auth;

import io.naraway.prologue.autoconfigure.PrologueProperties;
import io.naraway.prologue.support.rest.RestRequester;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InternalAuthProvider {
    //
    private static final String EXPIRATION = "expiration";

    @Value("${spring.application.name:drama}")
    private String applicationName;

    private final PrologueProperties properties;

    private RestRequester rest;
    private Map<String, String> tokens;
    private Timer timer;

    @PostConstruct
    private void initialize() {
        //
        PrologueProperties.InternalAuthProperties props = this.properties.getInternalAuth();

        this.rest = new RestRequester(this.properties.getInternalAuth().getRest());
        this.timer = new Timer();
        InternalAuthProvider self = this;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (self.tokens == null) {
                    self.issueToken();
                } else if (self.willExpired()) {
                    self.refreshToken();
                }
            }
        };
        this.timer.scheduleAtFixedRate(timerTask, 0, (long) props.getRefreshIntervalSeconds() * 1000);
        log.info("Internal auth store was configured");
    }

    @PreDestroy
    private void destroy() {
        //
        if (this.timer != null) {
            this.timer.cancel();
        }
        log.info("Internal auth store was destroyed");
    }

    public String getToken() {
        //
        if (this.tokens == null || !this.tokens.containsKey(OAuth2ParameterNames.ACCESS_TOKEN)) {
            return null;
        }

        return this.tokens.get(OAuth2ParameterNames.ACCESS_TOKEN);
    }

    private void issueToken() {
        //
        PrologueProperties.InternalAuthProperties props = this.properties.getInternalAuth();

        if (this.rest == null || props.getRest().isLoopback()) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put(OAuth2ParameterNames.SCOPE, "internal");
        data.put(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
        data.put(OAuth2ParameterNames.USERNAME, StringUtils.hasText(props.getUsername())
                ? props.getUsername() : String.format("internal-%s", applicationName));
        data.put(OAuth2ParameterNames.PASSWORD, props.getPassword());
        data.put(OAuth2ParameterNames.EXPIRES_IN, String.valueOf(60 * 60));

        MultiValueMap<String, String> form = new MultiValueMapAdapter<>(data.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue()))));

        Token token = this.rest.request(
                "/oauth/token", form, getOauthHeaders(), Token.class).block();

        storeTokenValues(token);
    }

    private void refreshToken() {
        //
        PrologueProperties.InternalAuthProperties props = this.properties.getInternalAuth();

        if (this.rest == null || props.getRest().isLoopback()) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        data.put(OAuth2ParameterNames.REFRESH_TOKEN, this.tokens.get(OAuth2ParameterNames.REFRESH_TOKEN));

        MultiValueMap<String, String> form = new MultiValueMapAdapter<>(data.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue()))));

        Token token = this.rest.request(
                "/oauth/token", form, getOauthHeaders(), Token.class).block();

        storeTokenValues(token);
    }

    private boolean willExpired() {
        //
        if (this.tokens == null || !this.tokens.containsKey(EXPIRATION)) {
            return true;
        }

        PrologueProperties.InternalAuthProperties props = this.properties.getInternalAuth();

        int expirationSeconds = Integer.parseInt(this.tokens.get(EXPIRATION));
        int currentTImeSeconds = (int) System.currentTimeMillis() / 1000;

        return (expirationSeconds - currentTImeSeconds) <= props.getRefreshIntervalSeconds();
    }

    private void storeTokenValues(Token token) {
        //
        if (token == null) {
            throw new IllegalStateException("Cannot found internal token");
        }

        if (this.tokens == null) {
            this.tokens = new HashMap<>();
        }

        int currentTimeSeconds = (int) System.currentTimeMillis() / 1000;
        this.tokens.put(OAuth2ParameterNames.ACCESS_TOKEN, token.access_token);
        this.tokens.put(OAuth2ParameterNames.REFRESH_TOKEN, token.refresh_token);
        this.tokens.put(EXPIRATION, String.valueOf(currentTimeSeconds + token.expires_in));
    }

    private Map<String, String> getOauthHeaders() {
        //
        PrologueProperties.InternalAuthProperties.Client props = this.properties.getInternalAuth().getClient();

        Map<String, String> header = new HashMap<>();
        String certification = Base64.getEncoder().encodeToString(
                String.format("%s:%s", props.getId(), props.getSecret()).getBytes());
        header.put(HttpHeaders.AUTHORIZATION, String.format("Basic %s", certification));
        header.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        return header;
    }

    @Data
    @SuppressWarnings("java:S116")
    private static class Token {
        //
        private String access_token;
        private String token_type;
        private String refresh_token;
        private int expires_in;
        private String scope;
        private String jti;

    }
}
