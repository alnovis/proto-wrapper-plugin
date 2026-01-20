package io.alnovis.protowrapper.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.context.annotation.RequestScope;
import io.alnovis.protowrapper.spring.context.FactoryBasedVersionContextProvider;
import io.alnovis.protowrapper.spring.context.ReflectiveVersionContextProvider;
import io.alnovis.protowrapper.spring.context.RequestScopedVersionContext;
import io.alnovis.protowrapper.spring.context.VersionContextProvider;
import io.alnovis.protowrapper.spring.web.ProtoWrapperExceptionHandler;
import io.alnovis.protowrapper.spring.web.VersionContextRequestFilter;

/**
 * Spring Boot auto-configuration for proto-wrapper.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link VersionContextProvider} - for resolving version contexts</li>
 *   <li>{@link RequestScopedVersionContext} - per-request version context holder</li>
 *   <li>{@link VersionContextRequestFilter} - extracts version from HTTP headers</li>
 *   <li>{@link ProtoWrapperExceptionHandler} - global exception handling</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>{@code
 * proto-wrapper:
 *   base-package: com.example.model.api
 *   versions:
 *     - v1
 *     - v2
 *   default-version: v2
 * }</pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(ProtoWrapperProperties.class)
@ConditionalOnProperty(
    prefix = "proto-wrapper",
    name = "base-package"
)
public class ProtoWrapperAutoConfiguration {

    /**
     * Creates the VersionContextProvider bean.
     *
     * <p>By default, uses {@link FactoryBasedVersionContextProvider} which delegates
     * to the generated VersionContextFactory for type-safe access.
     *
     * <p>Set {@code proto-wrapper.provider-type=reflective} to use the legacy
     * reflection-based provider for compatibility with older generated code.
     *
     * @param properties configuration properties
     * @return version context provider
     */
    @Bean
    @ConditionalOnMissingBean
    public VersionContextProvider versionContextProvider(ProtoWrapperProperties properties) {
        properties.validate();

        if (properties.getProviderType() == ProtoWrapperProperties.ProviderType.REFLECTIVE) {
            return new ReflectiveVersionContextProvider(
                properties.getBasePackage(),
                properties.getVersions(),
                properties.getEffectiveDefaultVersion()
            );
        }

        // Default: use factory-based provider
        return new FactoryBasedVersionContextProvider(properties.getBasePackage());
    }

    /**
     * Creates the request-scoped version context holder.
     *
     * @return request-scoped context
     */
    @Bean
    @RequestScope
    @ConditionalOnProperty(
        name = "proto-wrapper.request-scoped",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestScopedVersionContext requestScopedVersionContext() {
        return new RequestScopedVersionContext();
    }

    /**
     * Creates and registers the version context filter.
     *
     * @param requestScopedContext request-scoped context
     * @param provider version context provider
     * @param properties configuration properties
     * @return filter registration
     */
    @Bean
    @ConditionalOnProperty(
        name = "proto-wrapper.request-scoped",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<VersionContextRequestFilter> versionContextFilter(
            RequestScopedVersionContext requestScopedContext,
            VersionContextProvider provider,
            ProtoWrapperProperties properties) {

        FilterRegistrationBean<VersionContextRequestFilter> registration =
            new FilterRegistrationBean<>();

        registration.setFilter(
            new VersionContextRequestFilter(requestScopedContext, provider, properties)
        );
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("versionContextFilter");

        return registration;
    }

    /**
     * Creates the exception handler.
     *
     * @return exception handler
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "proto-wrapper.exception-handling",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnWebApplication
    public ProtoWrapperExceptionHandler protoWrapperExceptionHandler() {
        return new ProtoWrapperExceptionHandler();
    }
}
