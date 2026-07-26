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
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.argThat;
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
        when(mcpServiceDAO.searchByKeywordAndStatus("test", McpServiceStatus.DRAFT, pageable)).thenReturn(page);

        // when
        Page<McpService> result = mcpServiceService.listMcpServices("test", McpServiceStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(mcpServiceDAO).searchByKeywordAndStatus("test", McpServiceStatus.DRAFT, pageable);
    }

    @Nested
    @DisplayName("createMcpService - code validation")
    class CodeValidation {

        @Test
        @DisplayName("code 为空应抛出 MS_SERVICE_CODE_REQUIRED")
        void shouldRejectNullCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode(null);

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }

        @Test
        @DisplayName("code 为空白应抛出 MS_SERVICE_CODE_REQUIRED")
        void shouldRejectBlankCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("   ");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_REQUIRED);
        }

        @Test
        @DisplayName("code 包含大写字母应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectUpperCase() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("TestService");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 包含特殊字符应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectSpecialChars() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("test service!");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 包含中文字符应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectChineseChars() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("我的服务");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 以连字符开头应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectLeadingHyphen() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("-test");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 以连字符结尾应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectTrailingHyphen() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("test-");

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 长度超过 64 应抛出 MS_SERVICE_CODE_INVALID")
        void shouldRejectTooLong() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("a".repeat(65));

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_INVALID);
        }

        @Test
        @DisplayName("code 已存在应抛出 MS_SERVICE_CODE_EXISTS")
        void shouldRejectDuplicateCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("duplicate-code");
            when(mcpServiceDAO.existsByCode("duplicate-code")).thenReturn(true);

            DatiException ex = assertThrows(DatiException.class,
                () -> mcpServiceService.createMcpService(service));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MS_SERVICE_CODE_EXISTS);
        }

        @Test
        @DisplayName("合法 code 创建成功")
        void shouldAcceptValidCode() {
            McpService service = TestFixtures.createTestMcpService();
            service.setCode("my-service-01");
            when(mcpServiceDAO.existsByCode("my-service-01")).thenReturn(false);
            when(mcpServiceDAO.save(any())).thenAnswer(inv -> {
                McpServicePO po = inv.getArgument(0);
                po.setId(TestFixtures.TEST_MCP_SERVICE_ID);
                return po;
            });

            String id = mcpServiceService.createMcpService(service);
            assertThat(id).isEqualTo(TestFixtures.TEST_MCP_SERVICE_ID);
        }
    }

    @Nested
    @DisplayName("updateMcpService - code protection")
    class CodeProtection {

        @Test
        @DisplayName("更新时 code 字段应被忽略（不被修改）")
        void shouldNotUpdateCode() {
            McpServicePO existingPO = TestFixtures.createTestMcpServicePO();
            existingPO.setCode("original-code");
            when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(Optional.of(existingPO));

            McpService update = new McpService();
            update.setName("New Name");
            update.setCode("hacked-code");  // 尝试修改 code

            mcpServiceService.updateMcpService(TestFixtures.TEST_MCP_SERVICE_ID, update);

            // 确认 save 的 PO 中 code 未被修改
            verify(mcpServiceDAO).save(argThat(po -> "original-code".equals(po.getCode())));
        }
    }
}
