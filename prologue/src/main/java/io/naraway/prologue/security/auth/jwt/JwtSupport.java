package io.naraway.prologue.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtSupport {
    //
    private final JwtDecoder jwtDecoder;

    public Authentication resolveAuthentication(HttpServletRequest request) {
        //
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null &&
                authorization.startsWith(String.format("%s ", OAuth2AccessToken.TokenType.BEARER.getValue()))) {
            String token = authorization.substring(OAuth2AccessToken.TokenType.BEARER.getValue().length() + 1);
            return resolveAuthentication(token);
        }

        throw new BadCredentialsException("Invalid authorization header");
    }

    public Authentication resolveAuthentication(String token) {
        //
        try {
            Map<String, Object> claims = getClaims(token);
            Collection<? extends GrantedAuthority> authorities = Collections.emptyList(); // use custom authority
            UserDetails principal = User.builder()
                    .username((String) claims.get("username"))
                    .password("*")
                    .authorities(authorities)
                    .build();
            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    public boolean validate(HttpServletRequest request) {
        //
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null &&
                authorization.startsWith(String.format("%s ", OAuth2AccessToken.TokenType.BEARER.getValue()))) {
            String token = authorization.substring(OAuth2AccessToken.TokenType.BEARER.getValue().length() + 1);
            return validate(token);
        }

        return false;
    }

    public Map<String, Object> getClaims(HttpServletRequest request) {
        //
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null &&
                authorization.startsWith(String.format("%s ", OAuth2AccessToken.TokenType.BEARER.getValue()))) {
            String token = authorization.substring(OAuth2AccessToken.TokenType.BEARER.getValue().length() + 1);
            return getClaims(token);
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> getClaims(String token) {
        //
        if (StringUtils.hasText(token)) {
            return jwtDecoder.decode(token).getClaims();
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> getClaimHeaders(HttpServletRequest request) {
        //
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null &&
                authorization.startsWith(String.format("%s ", OAuth2AccessToken.TokenType.BEARER.getValue()))) {
            String token = authorization.substring(OAuth2AccessToken.TokenType.BEARER.getValue().length() + 1);
            return getClaimHeaders(token);
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> getClaimHeaders(String token) {
        //
        if (StringUtils.hasText(token)) {
            return jwtDecoder.decode(token).getHeaders();
        }

        return Collections.emptyMap();
    }

    public boolean validate(String token) {
        //
        if (StringUtils.hasText(token)) {
            try {
                jwtDecoder.decode(token);
                if (log.isTraceEnabled()) {
                    log.trace("Authorization token is VALIDATED");
                }
                return true;
            } catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.trace("Authorization token is NOT VALIDATED");
                }
                return false;
            }
        }
        return false;
    }
}
