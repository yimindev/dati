package com.dati.mcp.server.assembler;

import com.dati.auth.domain.service.UserService;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.server.pojo.McpServiceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServiceAssemblerTest {

    private McpServiceAssembler assembler;

    @Mock
    private McpToolService mcpToolService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        assembler = new McpServiceAssembler(mcpToolService);
        ReflectionTestUtils.setField(assembler, "userService", userService);
        when(userService.getUserMap(any())).thenReturn(Map.of());
        when(mcpToolService.countToolsByServiceId(anyString())).thenReturn(3L);
    }

    private McpService buildService() {
        McpService service = new McpService();
        service.setId("svc-1");
        service.setName("Test Service");
        service.setDescription("desc");
        service.setCode("test-service");
        service.setStatus(McpServiceStatus.DRAFT);
        return service;
    }

    @Test
    @DisplayName("Endpoint path - configured base URL returns full URL")
    void endpointPath_withBaseUrlConfigured_shouldReturnFullUrl() {
        ReflectionTestUtils.setField(assembler, "endpointBaseUrl", "https://mcp.example.com");

        McpServiceVO vo = assembler.toMcpServiceVO(buildService());

        assertThat(vo.getEndpointPath()).isEqualTo("https://mcp.example.com/test-service/mcp");
    }

    @Test
    @DisplayName("Endpoint path - base URL trailing slash is stripped")
    void endpointPath_withTrailingSlashBaseUrl_shouldNotDuplicateSlash() {
        ReflectionTestUtils.setField(assembler, "endpointBaseUrl", "https://mcp.example.com/");

        McpServiceVO vo = assembler.toMcpServiceVO(buildService());

        assertThat(vo.getEndpointPath()).isEqualTo("https://mcp.example.com/test-service/mcp");
    }

    @Test
    @DisplayName("Endpoint path - no base URL returns relative path")
    void endpointPath_withoutBaseUrl_shouldReturnRelativePath() {
        McpServiceVO vo = assembler.toMcpServiceVO(buildService());

        assertThat(vo.getEndpointPath()).isEqualTo("/test-service/mcp");
    }
}
