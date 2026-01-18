package space.alnovis.protowrapper.spring.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import space.alnovis.protowrapper.spring.ProtoWrapperProperties;
import space.alnovis.protowrapper.spring.context.RequestScopedVersionContext;
import space.alnovis.protowrapper.spring.context.VersionContextProvider;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionContextRequestFilterTest {

    @Mock
    private VersionContextProvider provider;

    @Mock
    private FilterChain filterChain;

    private RequestScopedVersionContext requestScopedContext;
    private ProtoWrapperProperties properties;
    private VersionContextRequestFilter filter;

    @BeforeEach
    void setUp() {
        requestScopedContext = new RequestScopedVersionContext();
        properties = new ProtoWrapperProperties();
        properties.setVersionHeader("X-Protocol-Version");

        filter = new VersionContextRequestFilter(requestScopedContext, provider, properties);
    }

    @Test
    void doFilterInternal_shouldExtractVersionFromHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "v2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object mockContext = new Object();
        when(provider.isSupported("v2")).thenReturn(true);
        when(provider.getContext("v2")).thenReturn(mockContext);

        filter.doFilterInternal(request, response, filterChain);

        assertTrue(requestScopedContext.isPresent());
        assertSame(mockContext, requestScopedContext.get());
        assertEquals("v2", requestScopedContext.getVersion());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenNoHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object mockContext = new Object();
        when(provider.getDefaultVersion()).thenReturn("v1");
        when(provider.getContext("v1")).thenReturn(mockContext);

        filter.doFilterInternal(request, response, filterChain);

        assertTrue(requestScopedContext.isPresent());
        assertSame(mockContext, requestScopedContext.get());
        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenHeaderIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object mockContext = new Object();
        when(provider.getDefaultVersion()).thenReturn("v1");
        when(provider.getContext("v1")).thenReturn(mockContext);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldFallbackToDefaultWhenVersionNotSupported() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "v99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object mockContext = new Object();
        when(provider.isSupported("v99")).thenReturn(false);
        when(provider.getDefaultVersion()).thenReturn("v1");
        when(provider.getContext("v1")).thenReturn(mockContext);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v1", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldTrimVersionHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Protocol-Version", "  v2  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object mockContext = new Object();
        when(provider.isSupported("v2")).thenReturn(true);
        when(provider.getContext("v2")).thenReturn(mockContext);

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

        Object mockContext = new Object();
        when(provider.isSupported("v2")).thenReturn(true);
        when(provider.getContext("v2")).thenReturn(mockContext);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("v2", requestScopedContext.getVersion());
    }

    @Test
    void doFilterInternal_shouldAlwaysCallFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(provider.getDefaultVersion()).thenReturn("v1");
        when(provider.getContext("v1")).thenReturn(new Object());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
