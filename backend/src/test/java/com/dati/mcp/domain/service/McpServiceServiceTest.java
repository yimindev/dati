package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.po.McpServicePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServiceService 单元测试")
class McpServiceServiceTest {

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @InjectMocks
    private McpServiceService mcpServiceService;

    private McpService testService;
    private McpServicePO testServicePO;

    @BeforeEach
    void setUp() {
        testService = TestFixtures.createTestMcpService();
        testServicePO = TestFixtures.createTestMcpServicePO();
    }

    @Test
    @DisplayName("创建 MCP 服务 - 成功")
    void createMcpService_shouldReturnId() {
        // given
        McpServicePO savedPO = new McpServicePO();
        savedPO.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        savedPO.setName(testService.getName());
        savedPO.setDescription(testService.getDescription());
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenReturn(savedPO);

        // when
        String result = mcpServiceService.createMcpService(testService);

        // then
        assertThat(result).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).save(any(McpServicePO.class));
    }

    @Test
    @DisplayName("更新 MCP 服务 - 成功")
    void updateMcpService_shouldUpdateSuccessfully() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenReturn(testServicePO);

        // when
        mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, testService);

        // then
        verify(mcpServiceDAO).findById(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).save(any(McpServicePO.class));
    }

    @Test
    @DisplayName("更新 MCP 服务 - 不存在时抛出异常")
    void updateMcpService_shouldThrowWhenNotFound() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.empty());

        // when & then
        DatiException exception = assertThrows(DatiException.class, () ->
            mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, testService)
        );
        assertThat(exception.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
        verify(mcpServiceDAO).findById(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO, never()).save(any());
    }

    @Test
    @DisplayName("查询 MCP 服务详情 - 成功")
    void getMcpService_shouldReturnService() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));

        // when
        McpService result = mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID);

        // then
        assertThat(result.getId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        assertThat(result.getName()).isEqualTo(testServicePO.getName());
    }

    @Test
    @DisplayName("查询 MCP 服务详情 - 不存在时抛出异常")
    void getMcpService_shouldThrowWhenNotFound() {
        // given
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.empty());

        // when & then
        DatiException exception = assertThrows(DatiException.class, () ->
            mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID)
        );
        assertThat(exception.getCode()).isEqualTo(ErrorCode.MS_SERVICE_NOT_FOUND);
    }

    @Test
    @DisplayName("分页查询 - 无筛选条件")
    void listMcpServices_withoutFilter() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAll(pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices(null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        verify(mcpServiceDAO).findAll(pageable);
    }

    @Test
    @DisplayName("分页查询 - 有关键词")
    void listMcpServices_withKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllByNameContainingOrId("test", "test", pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices("test", null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).findAllByNameContainingOrId("test", "test", pageable);
    }

    @Test
    @DisplayName("分页查询 - 有状态筛选")
    void listMcpServices_withStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllByStatus(McpServiceStatus.DRAFT, pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices(null, McpServiceStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).findAllByStatus(McpServiceStatus.DRAFT, pageable);
    }

    @Test
    @DisplayName("分页查询 - 有关键词和状态")
    void listMcpServices_withKeywordAndStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<McpServicePO> page = new PageImpl<>(List.of(testServicePO));
        when(mcpServiceDAO.findAllByNameContainingOrIdAndStatus("test", "test", McpServiceStatus.DRAFT, pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices("test", McpServiceStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).findAllByNameContainingOrIdAndStatus("test", "test", McpServiceStatus.DRAFT, pageable);
    }
}
