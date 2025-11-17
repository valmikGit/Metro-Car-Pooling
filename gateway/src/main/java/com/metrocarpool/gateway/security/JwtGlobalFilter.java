package com.metrocarpool.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Reached JwtGlobalFilter.filter.");

        String path = exchange.getRequest().getPath().value();

        // Allow auth endpoints to pass through (signup/signin handled by user-service)
        if (path.startsWith("/api/user/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(JwtConstant.JWT_HEADER);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or malformed Authorization header for path: {}", path);
            return unauthorized(exchange);
        }

        try {
            String token = authHeader.substring(7).trim();
            SecretKey secretKey = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
            // will throw JwtException on invalid/expired
            var jwt = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            var claims = jwt.getBody();
            log.debug("JWT validated for path={} ; claims={}", path, claims);

            // propagate a trusted header with the user's username (if present)
            String username = claims.get("username", String.class);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Username", username == null ? "" : username)
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange);
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        } catch (Exception e) {
            log.error("Unexpected error while validating JWT", e);
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        log.info("Reached JwtGlobalFilter.unauthorized.");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        log.info("Reached JwtGlobalFilter.getOrder.");

        // run early
        return Ordered.HIGHEST_PRECEDENCE;
    }
}