package com.enterprise.fintech.payment.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Enterprise BDNS (Banking Domain Name System) Resolver.
 *
 * Resolves private banking hostnames (*.fintech.enterprise.internal) across
 * secure banking intranet zones (Core Banking, Payment Switch, DMZ).
 */
@Component
public class BdnsServiceResolver {

    private static final Logger log = LoggerFactory.getLogger(BdnsServiceResolver.class);

    @Value("${bdns.domain.suffix:.fintech.enterprise.internal}")
    private String bdnsDomainSuffix = ".fintech.enterprise.internal";

    @Value("${cbs.weblogic.host:localhost}")
    private String cbsWeblogicFallbackHost = "localhost";

    private final Map<String, String> resolvedCache = new ConcurrentHashMap<>();

    public String resolveServiceHost(String fqdn) {
        if (fqdn == null || fqdn.isBlank()) {
            return "localhost";
        }

        return resolvedCache.computeIfAbsent(fqdn, host -> {
            try {
                // Attempt standard DNS / BDNS lookup
                InetAddress address = InetAddress.getByName(host);
                log.debug("[BDNS] Resolved {} to {}", host, address.getHostAddress());
                return address.getHostName();
            } catch (Exception ex) {
                // Graceful fallback for local development or non-enterprise networks
                log.warn("[BDNS Notice] Host '{}' not directly resolvable on local network. Routing via fallback '{}'",
                        host, cbsWeblogicFallbackHost);
                return cbsWeblogicFallbackHost;
            }
        });
    }

    public URI resolveEndpointUri(String baseUriString) {
        try {
            URI uri = URI.create(baseUriString);
            String resolvedHost = resolveServiceHost(uri.getHost());
            int port = uri.getPort();
            String path = uri.getPath() != null ? uri.getPath() : "";
            return URI.create(uri.getScheme() + "://" + resolvedHost + (port > 0 ? ":" + port : "") + path);
        } catch (Exception ex) {
            log.error("[BDNS] Failed to parse URI: {}", baseUriString, ex);
            return URI.create(baseUriString);
        }
    }
}
