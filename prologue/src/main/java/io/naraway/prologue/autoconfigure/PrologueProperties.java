package io.naraway.prologue.autoconfigure;

import io.naraway.accent.domain.context.UserType;
import io.naraway.prologue.security.web.xss.converter.XssConverterType;
import io.naraway.prologue.shared.dock.cache.DockCacheType;
import io.naraway.prologue.support.rest.RestRequesterProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Data
@ConfigurationProperties(prefix = "nara.prologue")
public class PrologueProperties {
    //
    // NOTE: To generate rsa key pair, check JwkTools
    @Value("${nara.jwt-signing.key-id:nara}")
    private String jwtSigningKeyId;
    @Value("${nara.jwt-signing.public-key:}")
    private String jwtSigningPublicKey;
    @Value("${nara.jwt-signing.private-key:}")
    private String jwtSigningPrivateKey;

    private boolean enabled;
    private StageRequestProperties defaultContext;
    private List<String> permitAllAntMatchers;
    private InternalAuthProperties internalAuth;
    private DockProperties dock;
    private XssSecureProperties xssSecure;
    private MethodSecureProperties methodSecure;

    public PrologueProperties() {
        //
        this.enabled = true;
        this.defaultContext = new StageRequestProperties();
        this.permitAllAntMatchers = new ArrayList<>();
        this.internalAuth = new InternalAuthProperties();
        this.dock = new DockProperties();
        this.xssSecure = new XssSecureProperties();
        this.methodSecure = new MethodSecureProperties();
    }

    @Data
    public static class StageRequestProperties {
        //
        private String username;
        private UserType userType;
        private String displayName;
        private String email;
        private boolean enabled;
        private String actorId;
        private String pavilionId;
        private List<String> cineroomIds;
        private String kollectionId;
        private List<String> roles;

        public StageRequestProperties() {
            //
            this.username = "user@naraway.io";
            this.userType = UserType.Citizen;
            this.displayName = "Nara Way";
            this.email = "user@naraway.io";
            this.enabled = true;
            this.actorId = "1@1:1:1:1-1";
            this.pavilionId = "1:1:1";
            this.cineroomIds = Arrays.asList("1:1:1:1", "1:1:1:2");
            this.kollectionId = "nara";
            this.roles = Arrays.asList("manager", "user");
        }
    }

    @Data
    public static class XssSecureProperties {
        //
        private boolean enabled;
        private List<String> excludeUrls;
        private XssConverterType converter;

        public XssSecureProperties() {
            //
            this.enabled = true;
            this.excludeUrls = new ArrayList<>();
            this.converter = XssConverterType.ESCAPE;
        }
    }

    @Data
    public static class MethodSecureProperties {
        //
        private boolean enabled;
        private List<String> methods;

        public MethodSecureProperties() {
            //
            this.enabled = true;
            this.methods = Arrays.asList(HttpMethod.OPTIONS.name(), HttpMethod.TRACE.name(), "COPY", "CONNECT");
        }
    }

    @Data
    public static class InternalAuthProperties {
        //
        private boolean enabled;
        private int refreshIntervalSeconds;
        private String username;
        private String password;
        private Client client;
        private RestRequesterProperties rest;

        public InternalAuthProperties() {
            //
            this.enabled = true;
            this.refreshIntervalSeconds = 60 * 5; // 5 Minutes
            this.password = "narasecret-internal";
            this.client = new Client();
            this.rest = RestRequesterProperties.builder()
                    .baseUrl("http://checkpoint:8080")
                    .maxMemorySize(1024 * 1024) // 1 MB
                    .build();
        }

        @Data
        public static class Client {
            //
            private String id;
            private String secret;

            public Client() {
                //
                this.id = "nara";
                this.secret = "narasecret";
            }
        }
    }

    @Data
    public static class DockProperties {
        //
        private boolean enabled;
        private DockCacheType cacheType;
        private int cacheTtlSeconds;
        private int cacheRefreshIntervalSeconds;
        private RestRequesterProperties rest;

        public DockProperties() {
            //
            this.enabled = true;
            this.cacheType = DockCacheType.EHCACHE;
            this.cacheTtlSeconds = 60 * 30; // 5 Minutes
            this.cacheRefreshIntervalSeconds = 20;
            this.rest = RestRequesterProperties.builder()
                    .baseUrl("http://metro:8080")
                    .maxMemorySize(1024 * 1024 * 10) // 10 MB
                    .build();
        }
    }
}
