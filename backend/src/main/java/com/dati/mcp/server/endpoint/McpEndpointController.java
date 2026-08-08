package com.dati.mcp.server.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP endpoint HTTP adapter. Only maps HTTP concerns (routing, headers, response
 * envelope); all protocol logic lives in {@link McpEndpointService}. Authentication is
 * handled by the global {@code AuthInterceptor} (registered for /{code}/mcp in
 * WebMvcConfig). Responses are MCP protocol types serialized by the SDK camelCase
 * mapper via {@code McpProtocolMessageConverter}.
 */
@RestController
public class McpEndpointController {

    private final McpEndpointService endpointService;

    public McpEndpointController(McpEndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping("/{code}/mcp")
    public ResponseEntity<Object> handle(@PathVariable String code, HttpServletRequest request,
                                         @RequestBody String body) {
        McpEndpointService.McpEndpointResult result = endpointService.handle(
            code, body, request.getHeader("Origin"), request.getHeader("MCP-Protocol-Version"));
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
