package com.dati.config;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

/**
 * Static resource hosting for the SPA and the VitePress help center. Always active -
 * whether anything is actually served depends solely on the static content on the
 * classpath (baked into the jar by the {@code standalone} profile). No controllers:
 * everything is declarative resource resolution.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Help center: VitePress clean URLs (/docs/faq -> faq.html, /docs/en/ -> en/index.html).
        // /docs/** is more specific than /**, so unknown docs pages 404 instead of falling back to the SPA.
        registry.addResourceHandler("/docs/**")
                .addResourceLocations("classpath:/static/docs/")
                .resourceChain(true)
                .addResolver(new DocsHtmlResolver());

        // SPA: history-mode fallback to index.html for unknown non-API, non-file paths.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // VitePress serves with base /docs/, so /docs must redirect to /docs/ for relative links to work.
        registry.addViewController("/docs").setViewName("redirect:/docs/");
        // Empty path-within-pattern (/docs/) is short-circuited by the resource handler
        // before resolvers run, so serve the docs index through a view controller instead.
        registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
    }

    /**
     * Resolves VitePress clean URLs against the docs directory. Exact hits (assets,
     * hashmap.json, *.html) are already resolved by the default resolver before this one.
     * <p>
     * Must be request-aware because PathPattern strips the trailing slash from the
     * path-within-pattern: /docs/en/ arrives here as "en", so the directory case is
     * detected from the request URI. The bare /docs/ (empty path-within-pattern) never
     * reaches this resolver - the handler short-circuits empty paths, so /docs/ is
     * forwarded to /docs/index.html by a view controller instead.
     */
    private static final class DocsHtmlResolver extends PathResourceResolver {

        @Override
        public Resource resolveResource(HttpServletRequest request, @NonNull String requestPath,
                                        @NonNull List<? extends Resource> locations, @NonNull ResourceResolverChain chain) {
            Resource exact = resolveExact(requestPath, locations);
            if (exact != null) {
                return exact;
            }
            String target;
            if (request.getRequestURI().endsWith("/")) {
                target = requestPath.isEmpty() ? "index.html" : requestPath + "/index.html";
            }
            else {
                target = requestPath + ".html";
            }
            return resolveExact(target, locations);
        }

        private Resource resolveExact(String path, List<? extends Resource> locations) {
            for (Resource location : locations) {
                try {
                    Resource resource = super.getResource(path, location);
                    if (resource != null) {
                        return resource;
                    }
                }
                catch (IOException ex) {
                    throw new IllegalStateException("Failed to resolve docs resource path [" + path + "]", ex);
                }
            }
            return null;
        }
    }

    /**
     * SPA history-mode fallback. Exact hits are already resolved by the default resolver
     * before this one; misses serve index.html unless they belong to an API root (must 404)
     * or look like a file (extension present, must 404).
     */
    private static final class SpaFallbackResolver extends PathResourceResolver {

        private static final Set<String> API_ROOTS =
                Set.of("v1", "api", "v3", "h2-console", "swagger-ui", "actuator");

        @Override
        protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
            Resource exact = super.getResource(resourcePath, location);
            if (exact != null) {
                return exact;
            }
            if (isApiRoot(resourcePath) || resourcePath.contains(".")) {
                return null;
            }
            return super.getResource("index.html", location);
        }

        private boolean isApiRoot(String path) {
            int slash = path.indexOf('/');
            String root = slash < 0 ? path : path.substring(0, slash);
            return API_ROOTS.contains(root);
        }
    }
}
