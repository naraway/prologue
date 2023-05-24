package io.naraway.prologue.support.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import io.naraway.accent.util.entity.Entities;
import io.naraway.accent.util.json.JsonUtil;
import io.naraway.prologue.shared.auth.InternalAuthProvider;
import io.naraway.prologue.shared.http.HttpHeaderProvider;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class RestRequester {
    //
    private final RestRequesterProperties properties;
    private final WebClient webClient;
    private final HeaderType headerType;

    private InternalAuthProvider internalAuthProvider;
    private Consumer<HttpHeaders> headerConsumer;

    public RestRequester(RestRequesterProperties properties) {
        //
        this.properties = properties;
        this.webClient = getWebClient();
        this.headerType = HeaderType.NONE;
    }

    public RestRequester(
            RestRequesterProperties properties, InternalAuthProvider internalAuthProvider) {
        //
        this.properties = properties;
        this.webClient = getWebClient();
        this.internalAuthProvider = internalAuthProvider;
        this.headerType = HeaderType.INTERNAL_AUTH;
    }

    public RestRequester(
            RestRequesterProperties properties, HttpHeaderProvider httpHeaderProvider) {
        //
        this.properties = properties;
        this.webClient = getWebClient();
        this.headerConsumer = httpHeaderProvider.getConsumer();
        this.headerType = HeaderType.HEADER_CONSUMER;
    }

    public RestRequester(
            RestRequesterProperties properties, Consumer<HttpHeaders> headerConsumer) {
        //
        this.properties = properties;
        this.webClient = getWebClient();
        this.headerConsumer = headerConsumer;
        this.headerType = HeaderType.HEADER_CONSUMER;
    }

    public Mono<String> request(HttpMethod method, String pahtname) {
        //
        if (this.properties.isLoopback()) {
            return Mono.empty().map(result -> "");
        }

        return this.requestServer(method, pahtname, null, null);
    }

    // @formatter:off
    public Mono<String> get   (String pahtname) { return this.request(HttpMethod.GET   , pahtname); }
    public Mono<String> post  (String pahtname) { return this.request(HttpMethod.POST  , pahtname); }
    public Mono<String> put   (String pahtname) { return this.request(HttpMethod.PUT   , pahtname); }
    public Mono<String> patch (String pahtname) { return this.request(HttpMethod.PATCH , pahtname); }
    public Mono<String> delete(String pahtname) { return this.request(HttpMethod.DELETE, pahtname); }
    // @formatter:on

    public Mono<String> request(HttpMethod method, String pahtname, Object data) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("");
        }

        return this.requestServer(method, pahtname, data, null);
    }

    // @formatter:off
    public Mono<String> get   (String pahtname, Object data) { return this.request(HttpMethod.GET   , pahtname, data); }
    public Mono<String> post  (String pahtname, Object data) { return this.request(HttpMethod.POST  , pahtname, data); }
    public Mono<String> put   (String pahtname, Object data) { return this.request(HttpMethod.PUT   , pahtname, data); }
    public Mono<String> patch (String pahtname, Object data) { return this.request(HttpMethod.PATCH , pahtname, data); }
    public Mono<String> delete(String pahtname, Object data) { return this.request(HttpMethod.DELETE, pahtname, data); }
    // @formatter:on

    public Mono<String> request(HttpMethod method, String pahtname, Map<String, String> header) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("");
        }

        return this.requestServer(method, pahtname, null, header);
    }

    // @formatter:off
    public Mono<String> get   (String pahtname, Map<String, String> header) { return this.request(HttpMethod.GET   , pahtname, header); }
    public Mono<String> post  (String pahtname, Map<String, String> header) { return this.request(HttpMethod.POST  , pahtname, header); }
    public Mono<String> put   (String pahtname, Map<String, String> header) { return this.request(HttpMethod.PUT   , pahtname, header); }
    public Mono<String> patch (String pahtname, Map<String, String> header) { return this.request(HttpMethod.PATCH , pahtname, header); }
    public Mono<String> delete(String pahtname, Map<String, String> header) { return this.request(HttpMethod.DELETE, pahtname, header); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        Mono<String> mono = this.requestServer(method, pathname, null, null);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, Class<T> clazz) { return this.request(HttpMethod.GET   , pahtname, clazz); }
    public <T> Mono<T> post  (String pahtname, Class<T> clazz) { return this.request(HttpMethod.POST  , pahtname, clazz); }
    public <T> Mono<T> put   (String pahtname, Class<T> clazz) { return this.request(HttpMethod.PUT   , pahtname, clazz); }
    public <T> Mono<T> patch (String pahtname, Class<T> clazz) { return this.request(HttpMethod.PATCH , pahtname, clazz); }
    public <T> Mono<T> delete(String pahtname, Class<T> clazz) { return this.request(HttpMethod.DELETE, pahtname, clazz); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, TypeReference<T> type) {
        //
        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        Mono<String> mono = this.requestServer(method, pathname, null, null);
        return mono.map(result -> JsonUtil.fromJson(result, type));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, TypeReference<T> type) { return this.request(HttpMethod.GET   , pahtname, type); }
    public <T> Mono<T> post  (String pahtname, TypeReference<T> type) { return this.request(HttpMethod.POST  , pahtname, type); }
    public <T> Mono<T> put   (String pahtname, TypeReference<T> type) { return this.request(HttpMethod.PUT   , pahtname, type); }
    public <T> Mono<T> patch (String pahtname, TypeReference<T> type) { return this.request(HttpMethod.PATCH , pahtname, type); }
    public <T> Mono<T> delete(String pahtname, TypeReference<T> type) { return this.request(HttpMethod.DELETE, pahtname, type); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        Mono<String> mono = this.requestServer(method, pathname, null, header);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    // @formatter:off
    public <T> Mono<T>    get(String pahtname, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.GET   , pahtname, header, clazz); }
    public <T> Mono<T>   post(String pahtname, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.POST  , pahtname, header, clazz); }
    public <T> Mono<T>    put(String pahtname, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.PUT   , pahtname, header, clazz); }
    public <T> Mono<T>  patch(String pahtname, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.PATCH , pahtname, header, clazz); }
    public <T> Mono<T> delete(String pahtname, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.DELETE, pahtname, header, clazz); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Map<String, String> header, TypeReference<T> type) {
        //
        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        Mono<String> mono = this.requestServer(method, pathname, null, header);
        return mono.map(result -> JsonUtil.fromJson(result, type));
    }

    // @formatter:off
    public <T> Mono<T>    get(String pahtname, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.GET   , pahtname, header, type); }
    public <T> Mono<T>   post(String pahtname, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.POST  , pahtname, header, type); }
    public <T> Mono<T>    put(String pahtname, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.PUT   , pahtname, header, type); }
    public <T> Mono<T>  patch(String pahtname, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.PATCH , pahtname, header, type); }
    public <T> Mono<T> delete(String pahtname, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.DELETE, pahtname, header, type); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Object data, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        Mono<String> mono = this.requestServer(method, pathname, data, null);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, Object data, Class<T> clazz) { return this.request(HttpMethod.GET   , pahtname, data, clazz); }
    public <T> Mono<T> post  (String pahtname, Object data, Class<T> clazz) { return this.request(HttpMethod.POST  , pahtname, data, clazz); }
    public <T> Mono<T> put   (String pahtname, Object data, Class<T> clazz) { return this.request(HttpMethod.PUT   , pahtname, data, clazz); }
    public <T> Mono<T> patch (String pahtname, Object data, Class<T> clazz) { return this.request(HttpMethod.PATCH , pahtname, data, clazz); }
    public <T> Mono<T> delete(String pahtname, Object data, Class<T> clazz) { return this.request(HttpMethod.DELETE, pahtname, data, clazz); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Object data, TypeReference<T> type) {
        //
        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        Mono<String> mono = this.requestServer(method, pathname, data, null);
        return mono.map(result -> JsonUtil.fromJson(result, type));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, Object data, TypeReference<T> type) { return this.request(HttpMethod.GET   , pahtname, data, type); }
    public <T> Mono<T> post  (String pahtname, Object data, TypeReference<T> type) { return this.request(HttpMethod.POST  , pahtname, data, type); }
    public <T> Mono<T> put   (String pahtname, Object data, TypeReference<T> type) { return this.request(HttpMethod.PUT   , pahtname, data, type); }
    public <T> Mono<T> patch (String pahtname, Object data, TypeReference<T> type) { return this.request(HttpMethod.PATCH , pahtname, data, type); }
    public <T> Mono<T> delete(String pahtname, Object data, TypeReference<T> type) { return this.request(HttpMethod.DELETE, pahtname, data, type); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Object data, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        Mono<String> mono = this.requestServer(method, pathname, data, header);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.GET   , pahtname, data, header, clazz); }
    public <T> Mono<T> post  (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.POST  , pahtname, data, header, clazz); }
    public <T> Mono<T> put   (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.PUT   , pahtname, data, header, clazz); }
    public <T> Mono<T> patch (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.PATCH , pahtname, data, header, clazz); }
    public <T> Mono<T> delete(String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.request(HttpMethod.DELETE, pahtname, data, header, clazz); }
    // @formatter:on

    public <T> Mono<T> request(HttpMethod method, String pathname, Object data, Map<String, String> header, TypeReference<T> type) {
        //
        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        Mono<String> mono = this.requestServer(method, pathname, data, header);
        return mono.map(result -> JsonUtil.fromJson(result, type));
    }

    // @formatter:off
    public <T> Mono<T> get   (String pahtname, Object data, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.GET   , pahtname, data, header, type); }
    public <T> Mono<T> post  (String pahtname, Object data, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.POST  , pahtname, data, header, type); }
    public <T> Mono<T> put   (String pahtname, Object data, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.PUT   , pahtname, data, header, type); }
    public <T> Mono<T> patch (String pahtname, Object data, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.PATCH , pahtname, data, header, type); }
    public <T> Mono<T> delete(String pahtname, Object data, Map<String, String> header, TypeReference<T> type) { return this.request(HttpMethod.DELETE, pahtname, data, header, type); }
    // @formatter:on

    public <T> Mono<List<T>> requestList(HttpMethod method, String pathname, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        Mono<String> mono = this.requestServer(method, pathname, null, null);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    // @formatter:off
    public <T> Mono<List<T>> getList   (String pahtname, Class<T> clazz) { return this.requestList(HttpMethod.GET   , pahtname, clazz); }
    public <T> Mono<List<T>> postList  (String pahtname, Class<T> clazz) { return this.requestList(HttpMethod.POST  , pahtname, clazz); }
    public <T> Mono<List<T>> putList   (String pahtname, Class<T> clazz) { return this.requestList(HttpMethod.PUT   , pahtname, clazz); }
    public <T> Mono<List<T>> patchList (String pahtname, Class<T> clazz) { return this.requestList(HttpMethod.PATCH , pahtname, clazz);}
    public <T> Mono<List<T>> deleteList(String pahtname, Class<T> clazz) { return this.requestList(HttpMethod.DELETE, pahtname, clazz); }
    // @formatter:on

    public <T> Mono<List<T>> requestList(HttpMethod method, String pathname, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        Mono<String> mono = this.requestServer(method, pathname, null, header);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    // @formatter:off
    public <T> Mono<List<T>> getList   (String pahtname, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.GET   , pahtname, header, clazz); }
    public <T> Mono<List<T>> postList  (String pahtname, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.POST  , pahtname, header, clazz); }
    public <T> Mono<List<T>> putList   (String pahtname, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.PUT   , pahtname, header, clazz); }
    public <T> Mono<List<T>> patchList (String pahtname, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.PATCH , pahtname, header, clazz); }
    public <T> Mono<List<T>> deleteList(String pahtname, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.DELETE, pahtname, header, clazz); }
    // @formatter:on

    public <T> Mono<List<T>> requestList(HttpMethod method, String pathname, Object data, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        Mono<String> mono = this.requestServer(method, pathname, data, null);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    // @formatter:off
    public <T> Mono<List<T>> getList   (String pahtname, Object data, Class<T> clazz) { return this.requestList(HttpMethod.GET   , pahtname, data, clazz); }
    public <T> Mono<List<T>> postList  (String pahtname, Object data, Class<T> clazz) { return this.requestList(HttpMethod.POST  , pahtname, data, clazz); }
    public <T> Mono<List<T>> putList   (String pahtname, Object data, Class<T> clazz) { return this.requestList(HttpMethod.PUT   , pahtname, data, clazz); }
    public <T> Mono<List<T>> patchList (String pahtname, Object data, Class<T> clazz) { return this.requestList(HttpMethod.PATCH , pahtname, data, clazz); }
    public <T> Mono<List<T>> deleteList(String pahtname, Object data, Class<T> clazz) { return this.requestList(HttpMethod.DELETE, pahtname, data, clazz); }
    // @formatter:on

    public <T> Mono<List<T>> requestList(
            HttpMethod method, String pathname, Object data, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        Mono<String> mono = this.requestServer(method, pathname, data, header);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    // @formatter:off
    public <T> Mono<List<T>> getList   (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.GET   , pahtname, data, header, clazz); }
    public <T> Mono<List<T>> postList  (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.POST  , pahtname, data, header, clazz); }
    public <T> Mono<List<T>> putList   (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.PUT   , pahtname, data, header, clazz); }
    public <T> Mono<List<T>> patchList (String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.PATCH , pahtname, data, header, clazz); }
    public <T> Mono<List<T>> deleteList(String pahtname, Object data, Map<String, String> header, Class<T> clazz) { return this.requestList(HttpMethod.DELETE, pahtname, data, header, clazz); }
    // @formatter:on

    public <T> Mono<T> request(String pathname, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, null, null);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    public <T> Mono<T> request(String pathname, Object data, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, data, null);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    public <T> Mono<T> request(String pathname, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, null, header);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    public <T> Mono<T> request(String pathname, Object data, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Entities.generateStub(clazz));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, data, header);
        return mono.map(result -> JsonUtil.fromJson(result, clazz));
    }

    public <T> Mono<List<T>> requestList(String pathname, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, null, null);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    public <T> Mono<List<T>> requestList(String pathname, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, null, header);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    public <T> Mono<List<T>> requestList(String pathname, Object data, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, data, null);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    public <T> Mono<List<T>> requestList(String pathname, Object data, Map<String, String> header, Class<T> clazz) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just("").map(result -> Arrays.asList((T) Entities.generateStub(clazz)));
        }

        HttpMethod method = this.properties.getBaseMethod();
        Mono<String> mono = this.requestServer(method, pathname, data, header);
        return mono.map(result -> JsonUtil.fromJsonList(result, clazz));
    }

    @SuppressWarnings("java:S3740")
    private Mono<String> requestServer(HttpMethod method, String pathname, Object data, Map<String, String> header) {
        //
        // https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods

        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        if (log.isTraceEnabled()) {
            if (data == null) {
                log.trace("Request, method = {}, uri = {}", method, pathname);
            } else {
                log.trace("Request, method = {}, uri = {}, data = {}", method, pathname, JsonUtil.toJson(data.toString()));
            }
        }

        Map<String, String> requestHeader = new HashMap<>();
        if (header != null) {
            requestHeader.putAll(header);
        }
        BodyInserter bodyInserter = null;
        if (data == null) {
            bodyInserter = BodyInserters.empty();
        } else if (MultiValueMap.class.isAssignableFrom(data.getClass())) {
            if (requestHeader.containsKey(HttpHeaders.CONTENT_TYPE) &&
                    requestHeader.get(HttpHeaders.CONTENT_TYPE).equals(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                bodyInserter = BodyInserters.fromMultipartData((MultiValueMap) data);
            } else {
                bodyInserter = BodyInserters.fromFormData((MultiValueMap) data);
                requestHeader.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            }
        } else {
            bodyInserter = BodyInserters.fromValue(data);
        }

        return this.webClient
                .method(method)
                .uri(pathname)
                .headers(getRequestHeaders(requestHeader))
                .body(bodyInserter)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.NOT_FOUND.value(), response -> Mono.empty())
                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.NO_CONTENT.value(), response -> Mono.empty())
                .onStatus(HttpStatus::is4xxClientError, response -> response.createException().flatMap(it -> {
                    log.warn("Client error, pathname = {}, data = {}, header = {}",
                            pathname, JsonUtil.toJson(data), JsonUtil.toJson(requestHeader), it);
                    return response.bodyToMono(String.class).map(RuntimeException::new);
                }))
                .onStatus(HttpStatus::is5xxServerError, response -> response.createException().flatMap(it -> {
                    log.warn("Client error, pathname = {}, data = {}, header = {}",
                            pathname, JsonUtil.toJson(data), JsonUtil.toJson(requestHeader), it);
                    return response.bodyToMono(String.class).map(RuntimeException::new);
                }))
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty())
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable)))
                .retry(this.properties.getRetry());
    }

    public Mono<byte[]> getBinary(String pathname, Map<String, String> header) {
        //
        if (this.properties.isLoopback()) {
            return Mono.just(new byte[0]);
        }

        return requestServerBinary(pathname, header);
    }

    private Mono<byte[]> requestServerBinary(String pathname, Map<String, String> header) {
        //
        // https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods

        if (this.properties.isLoopback()) {
            return Mono.empty();
        }

        if (log.isTraceEnabled()) {
            log.trace("get binary, uri = {}", pathname);
        }

        Map<String, String> requestHeader = new HashMap<>();
        if (header != null) {
            requestHeader.putAll(header);
        }

        return this.webClient
                .get()
                .uri(pathname)
                .headers(getRequestHeaders(requestHeader))
                .exchangeToMono(response -> response.bodyToMono(ByteArrayResource.class))
                .map(ByteArrayResource::getByteArray)
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty())
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable)))
                .retry(this.properties.getRetry());
    }

    private Consumer<HttpHeaders> getRequestHeaders(Map<String, String> header) {
        //
        return httpHeaders -> {
            if (header != null) {
                header.entrySet().forEach(entry -> httpHeaders.set(entry.getKey(), entry.getValue()));
            }

            if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            }

            switch (headerType) {
                case NONE:
                    break;
                case INTERNAL_AUTH:
                    if (this.internalAuthProvider != null) {
                        httpHeaders.set(HttpHeaders.AUTHORIZATION, getBearerToken());
                    }
                    break;
                case HEADER_CONSUMER:
                    if (this.headerConsumer != null) {
                        this.headerConsumer.accept(httpHeaders);
                    }
                    break;
            }
        };
    }

    private String getBearerToken() {
        //
        return String.format("%s %s", OAuth2AccessToken.TokenType.BEARER.getValue(), this.internalAuthProvider.getToken());
    }

    private WebClient getWebClient() {
        //
        RestRequesterProperties props = this.properties;

        if (!StringUtils.hasText(props.getBaseUrl()) || props.isLoopback()) {
            return null;
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getRequestTimeoutSeconds() * 1000)
                .responseTimeout(Duration.ofSeconds(props.getRequestTimeoutSeconds()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(props.getRequestTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.getRequestTimeoutSeconds(), TimeUnit.SECONDS)));

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(props.getMaxMemorySize())).build();

        return WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(props.getBaseUrl())
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    private enum HeaderType {
        //
        INTERNAL_AUTH,
        HEADER_CONSUMER,
        NONE
    }
}
