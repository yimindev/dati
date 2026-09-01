package com.dati.config.converter;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Serializes MCP protocol messages ({@link McpSchema.JSONRPCMessage} subtypes) with the
 * SDK camelCase ObjectMapper. The global DatI ObjectMapper is configured with
 * SNAKE_CASE for the REST API ({@code spring.jackson.property-naming-strategy}), which
 * would corrupt protocol field names like {@code protocolVersion}/{@code inputSchema}.
 *
 * <p>Scoped strictly to MCP protocol types: {@link #canWrite} only matches
 * {@link McpSchema.JSONRPCMessage}, so all other responses keep using the default
 * converters. Write-only ({@link #canRead} always false): request bodies are consumed
 * as raw String by the MCP endpoint controller.
 *
 * <p>Accept is intentionally not validated (the endpoint always responds JSON), so the
 * converter supports any media type to keep Spring content negotiation from rejecting
 * MCP messages based on the client's Accept header.
 */
public class McpProtocolMessageConverter extends MappingJackson2HttpMessageConverter {

    public McpProtocolMessageConverter() {
        super(((JacksonMcpJsonMapper) McpJsonDefaults.getMapper()).getObjectMapper());
        setSupportedMediaTypes(List.of(MediaType.ALL));
    }

    @Override
    public boolean canRead(@NonNull Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Type type, @NonNull Class<?> clazz, MediaType mediaType) {
        return McpSchema.JSONRPCMessage.class.isAssignableFrom(clazz)
            && super.canWrite(type, clazz, mediaType);
    }
}
