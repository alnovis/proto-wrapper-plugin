package io.alnovis.protowrapper.spring.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import io.alnovis.protowrapper.spring.ProtoWrapperProperties;
import io.alnovis.protowrapper.spring.context.RequestScopedVersionContext;
import io.alnovis.protowrapper.spring.context.VersionContextProvider;

import java.io.IOException;

/**
 * Servlet filter that extracts protocol version from HTTP headers and
 * initializes the request-scoped VersionContext.
 *
 * <p>Version resolution order:
 * <ol>
 *   <li>X-Protocol-Version header (or configured header name)</li>
 *   <li>Default version from configuration</li>
 * </ol>
 */
public class VersionContextRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(VersionContextRequestFilter.class);

    private final RequestScopedVersionContext requestScopedContext;
    private final VersionContextProvider provider;
    private final String versionHeader;

    /**
     * Creates a new VersionContextRequestFilter.
     *
     * @param requestScopedContext request-scoped context holder
     * @param provider version context provider
     * @param properties configuration properties
     */
    public VersionContextRequestFilter(
            RequestScopedVersionContext requestScopedContext,
            VersionContextProvider provider,
            ProtoWrapperProperties properties) {
        this.requestScopedContext = requestScopedContext;
        this.provider = provider;
        this.versionHeader = properties.getVersionHeader();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String version = extractVersion(request);
        Object context = provider.getContext(version);

        requestScopedContext.set(context, version);

        if (log.isDebugEnabled()) {
            log.debug("Set VersionContext for request {}: version={}",
                request.getRequestURI(), version);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts version from request header or falls back to default.
     *
     * @param request HTTP request
     * @return version string
     */
    private String extractVersion(HttpServletRequest request) {
        String headerValue = request.getHeader(versionHeader);

        if (headerValue != null && !headerValue.isBlank()) {
            String version = headerValue.trim();
            if (provider.isSupported(version)) {
                return version;
            }
            log.warn("Unsupported version '{}' in header {}, falling back to default",
                version, versionHeader);
        }

        // Fallback to default
        return provider.getDefaultVersion();
    }
}
