package com.banking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CHANNEL_HEADER = "X-Channel";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Channel header (iOS, Android, Web)
        String channel = request.getHeaders().getFirst(CHANNEL_HEADER);
        if (channel == null || channel.isBlank()) {
            String userAgent = request.getHeaders().getFirst("User-Agent");
            channel = resolveChannelFromUserAgent(userAgent);
        }

        final String finalCorrelationId = correlationId;
        final String finalChannel = channel;

        log.debug("Incoming API Gateway request [path: {}, method: {}, correlationId: {}, channel: {}]",
                request.getPath(), request.getMethod(), finalCorrelationId, finalChannel);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .header(CHANNEL_HEADER, finalChannel)
                .build();

        // Mutate response headers so client receives tracking correlation ID
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String resolveChannelFromUserAgent(String userAgent) {
        if (userAgent == null) return "WEB";
        String ua = userAgent.toLowerCase();
        if (ua.contains("cfnetwork") || ua.contains("darwin") || ua.contains("iphone") || ua.contains("ipad")) {
            return "IOS";
        } else if (ua.contains("android")) {
            return "ANDROID";
        }
        return "WEB";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
