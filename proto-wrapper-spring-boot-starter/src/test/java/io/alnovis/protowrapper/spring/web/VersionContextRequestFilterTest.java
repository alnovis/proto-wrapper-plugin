package io.alnovis.protowrapper.spring.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import io.alnovis.protowrapper.spring.ProtoWrapperProperties;
import io.alnovis.protowrapper.spring.context.RequestScopedVersionContext;
import io.alnovis.protowrapper.spring.context.VersionContextProvider;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VersionContextRequestFilterTest {

    private TestVersionContextProvider provider;
    private TestFilterChain filterChain;
    private RequestScopedVersionContext requestScopedContext;
    private ProtoWrapperProperties properties;
    private VersionContextRequestFilter filter;

    @BeforeEach
    void setUp() {
        provider = new TestVersionContextProvider();
        filterChain = new TestFilterChain();
        requestScopedContext = new RequestScopedVersionContext();
        properties = new ProtoWrapperProperties();
        properties.setVersionHeader("X-Protocol-Version");

        filter = new VersionContextRequestFilter(requestScopedContext, provider, properties);
    }

    // Test implementation of VersionContextProvider
    static class TestVersionContextProvider implements VersionContextProvider {
        private final Object v1Context = new Object();
        private final Object v2Context = new Object();

        @Override
        public Object getContext(String version) {
            return "v2".equals(version) ? v2Context : v1Context;
        }

        @Override
        public Optional<Object> findContext(String version) {
            return Optional.of(getContext(version));
        }

        @Override
        public Object getDefaultContext() {
            return v1Context;
        }

        @Override
        public List<String> getSupportedVersions() {
            return List.of("v1", "v2");
        }

        @Override
        public String getDefaultVersion() {
            return "v1";
        }

        @Override
        public boolean isSupported(String version) {
            return "v1".equals(version) || "v2".equals(version);
        }

        Object getV1Context() { return v1Context; }
        Object getV2Context() { return v2Context; }
    }

    // Test implementation of FilterChain
    static class TestFilterChain implements FilterChain {
        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            callCount.incrementAndGet();
        }

        int getCallCount() { return callCount.get(); }
    }

    @Test
    void doFilterInternal_shouldExtractVersionFromHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "v2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertTrue(requestScopedContext.isPresent());
        assertSame(provider.getV2Context(), requestScopedContext.get());
        assertEquals("v2", requestScopedContext.getVersion());
        assertEquals(1, filterChain.getCallCount());
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenNoHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertTrue(requestScopedContext.isPresent());
        assertSame(provider.getV1Context(), requestScopedContext.get());
        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenHeaderIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenVersionNotSupported() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "v99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldTrimVersionHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "  v2  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v2", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldUseCustomHeaderName() throws ServletException, IOException {
        properties.setVersionHeader("X-Custom-Version");
        filter = new VersionContextRequestFilter(requestScopedContext, provider, properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom-Version", "v2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v2", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldAlwaysCallFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(1, filterChain.getCallCount());
    }
}
