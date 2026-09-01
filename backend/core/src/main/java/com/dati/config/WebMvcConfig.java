package com.dati.config;

import com.dati.config.Interceptor.AuthInterceptor;
import com.dati.config.converter.McpProtocolMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private static final String[] AUTH_EXCLUDE_PATHS = {
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/public/**"
    };

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**", "/*/mcp")
                .excludePathPatterns(AUTH_EXCLUDE_PATHS);
    }

    /** MCP protocol messages need the SDK camelCase mapper; see {@link McpProtocolMessageConverter}. */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addFirst(new McpProtocolMessageConverter());
    }
}
