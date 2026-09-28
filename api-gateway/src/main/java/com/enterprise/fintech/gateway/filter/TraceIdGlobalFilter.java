package com.enterprise.fintech.gateway.filter;

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
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String activeTraceId = traceId;
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_HEADER, activeTraceId)
                .build();

        log.info("[Gateway] Incoming request {} {} with TraceId: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                activeTraceId);

        exchange.getResponse().getHeaders().set(TRACE_HEADER, activeTraceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> log.info("[Gateway] Completed request {} with TraceId: {} Status: {}",
                        exchange.getRequest().getURI().getPath(),
                        activeTraceId,
                        exchange.getResponse().getStatusCode()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
