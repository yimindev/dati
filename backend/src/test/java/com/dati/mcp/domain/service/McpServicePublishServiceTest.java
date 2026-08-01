package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpPrebuiltToolConfig;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.dao.McpServiceSnapshotDAO;
import com.dati.mcp.repository.mapper.McpServiceSnapshotMapper;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import com.dati.mcp.server.pojo.McpServiceDiffVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpServicePublishServiceTest {

    @Mock
    private McpServiceDAO mcpServiceDAO;

    @Mock
    private McpServiceSnapshotDAO snapshotDAO;

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private McpToolService toolService;

    @Mock
    private McpPromptService promptService;

    @InjectMocks
    private McpServicePublishService publishService;

    private McpServicePO testServicePO;

    @BeforeEach
    void setUp() {
        testServicePO = TestFixtures.createTestMcpServicePO();
    }

    @Test
    @DisplayName("Publish service - creates v1 snapshot and sets status to PUBLISHED")
    void publish_FirstTime_CreatesV1Snapshot() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findMaxVersionNumberByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(null);
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        when(snapshotDAO.save(any(McpServiceSnapshotPO.class))).thenAnswer(inv -> {
            McpServiceSnapshotPO po = inv.getArgument(0);
            po.setId("snap-001");
            return po;
        });
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        McpServiceSnapshot snapshot = publishService.publish(TestFixtures.TEST_MCP_SERVICE_ID, "Initial Release");

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getVersionNumber()).isEqualTo(1);
        assertThat(snapshot.getReleaseNote()).isEqualTo("Initial Release");
        assertThat(testServicePO.getStatus()).isEqualTo(McpServiceStatus.PUBLISHED);
        assertThat(testServicePO.getActiveVersionNumber()).isEqualTo(1);
        assertThat(testServicePO.getActiveVersionId()).isEqualTo("snap-001");
    }

    @Test
    @DisplayName("Publish again - version number increments to v2")
    void publish_SecondTime_IncrementsVersionNumber() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionNumber(1);
        testServicePO.setActiveVersionId("snap-001");

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findMaxVersionNumberByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(1);
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        when(snapshotDAO.save(any(McpServiceSnapshotPO.class))).thenAnswer(inv -> {
            McpServiceSnapshotPO po = inv.getArgument(0);
            po.setId("snap-002");
            return po;
        });
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        McpServiceSnapshot snapshot = publishService.publish(TestFixtures.TEST_MCP_SERVICE_ID, "Update v2");

        assertThat(snapshot.getVersionNumber()).isEqualTo(2);
        assertThat(testServicePO.getActiveVersionNumber()).isEqualTo(2);
        assertThat(testServicePO.getActiveVersionId()).isEqualTo("snap-002");
    }

    @Test
    @DisplayName("Disable service - status changes from PUBLISHED to DISABLED")
    void disable_ServiceIsPublished_ChangesStatusToDisabled() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionNumber(1);
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        publishService.disable(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(testServicePO.getStatus()).isEqualTo(McpServiceStatus.DISABLED);
    }

    @Test
    @DisplayName("Enable service - status restores from DISABLED to PUBLISHED")
    void enable_ServiceIsDisabled_RestoresStatusToPublished() {
        testServicePO.setStatus(McpServiceStatus.DISABLED);
        testServicePO.setActiveVersionNumber(1);
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        publishService.enable(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(testServicePO.getStatus()).isEqualTo(McpServiceStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Draft never published - getDiff returns hasChanges true")
    void getDiff_DraftNeverPublished_HasChangesIsTrue() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isHasChanges()).isTrue();
    }

    @Test
    @DisplayName("Modified draft - getDiff computes Tools/Prompts/Scope change details")
    void getDiff_DraftModified_CalculatesDiffDetails() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo sInfo = new McpServiceSnapshot.ServiceInfo();
        sInfo.setId("mcp-svc-001");
        sInfo.setName("Test MCP Service");
        sInfo.setDescription("Old desc");
        sInfo.setCode("test-mcp-service");
        content.setServiceInfo(sInfo);
        content.setDataScopes(Collections.emptyList());
        content.setPrebuiltTools(Collections.emptyList());
        content.setCustomTools(Collections.emptyList());
        content.setPrompts(Collections.emptyList());

        McpServiceSnapshotPO snapshotPO = new McpServiceSnapshotPO();
        snapshotPO.setId("snap-001");
        snapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPO.setVersionNumber(1);
        snapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(content));

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findById("snap-001")).thenReturn(Optional.of(snapshotPO));

        McpCustomTool tool = TestFixtures.createTestCustomTool();
        McpPrompt prompt = TestFixtures.createTestMcpPrompt();
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), List.of(tool)));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(prompt));

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isHasChanges()).isTrue();
        assertThat(diff.isToolsChanged()).isTrue();
        assertThat(diff.getAddedTools()).contains("list_tasks");
        assertThat(diff.isPromptsChanged()).isTrue();
        assertThat(diff.getAddedPrompts()).contains("analyze_table");
    }

    @Test
    @DisplayName("Service not found - throws MS_SERVICE_NOT_FOUND")
    void publish_NotFound_ThrowsDatiException() {
        when(mcpServiceDAO.findById("non-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publishService.publish("non-exist", "note"))
                .isInstanceOf(DatiException.class);
    }

    @Test
    @DisplayName("Publish service - empty data scope rejected (MS019)")
    void publish_EmptyDataScope_Throws() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> publishService.publish(TestFixtures.TEST_MCP_SERVICE_ID, "note"))
                .isInstanceOf(DatiException.class)
                .extracting(e -> ((DatiException) e).getCode())
                .isEqualTo(ErrorCode.MS_SERVICE_DATA_SCOPE_EMPTY);
    }

    // ── diff：审计字段差异不应误报变更 ──

    @Test
    @DisplayName("Prebuilt config changed - tools_changed and modified_tools contains tool names")
    void getDiff_PrebuiltChanged_ReportsModifiedTools() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        // 快照：SEARCH_METADATA timeout=30（默认）
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo sInfo = new McpServiceSnapshot.ServiceInfo();
        sInfo.setId("mcp-svc-001");
        sInfo.setName("Test MCP Service");
        sInfo.setDescription("Test MCP service for unit tests");
        sInfo.setCode("test-mcp-service");
        content.setServiceInfo(sInfo);
        content.setDataScopes(Collections.emptyList());
        content.setCustomTools(Collections.emptyList());
        content.setPrompts(Collections.emptyList());
        McpPrebuiltToolConfig snapshotPrebuilt = new McpPrebuiltToolConfig();
        snapshotPrebuilt.setId("pre-001");
        snapshotPrebuilt.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPrebuilt.setToolType(McpToolType.SEARCH_METADATA);
        snapshotPrebuilt.setEnabled(true);
        snapshotPrebuilt.setConfig(new ToolConfig.SearchMetadataConfig());
        content.setPrebuiltTools(List.of(McpServiceSnapshotMapper.toPrebuiltToolDraft(snapshotPrebuilt)));

        McpServiceSnapshotPO snapshotPO = new McpServiceSnapshotPO();
        snapshotPO.setId("snap-001");
        snapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPO.setVersionNumber(1);
        snapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(content));

        // 草稿：SEARCH_METADATA timeout=60（真实变更）
        McpPrebuiltToolConfig currentPrebuilt = new McpPrebuiltToolConfig();
        currentPrebuilt.setId("pre-001");
        currentPrebuilt.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        currentPrebuilt.setToolType(McpToolType.SEARCH_METADATA);
        currentPrebuilt.setEnabled(true);
        ToolConfig.SearchMetadataConfig cfg = new ToolConfig.SearchMetadataConfig();
        cfg.setTimeout(60);
        currentPrebuilt.setConfig(cfg);

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findById("snap-001")).thenReturn(Optional.of(snapshotPO));
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(new ToolsResult(List.of(currentPrebuilt), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isToolsChanged()).isTrue();
        assertThat(diff.getModifiedTools()).contains("SEARCH_METADATA");
    }

    @Test
    @DisplayName("Prebuilt only differs in audit fields (no real change) - diff has no changes")
    void getDiff_PrebuiltOnlyAuditFieldsDiffer_NoChanges() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        // 快照：SEARCH_METADATA 带 id，但无时间戳（parseContent 反序列化丢弃审计字段）
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo sInfo = new McpServiceSnapshot.ServiceInfo();
        sInfo.setId("mcp-svc-001");
        sInfo.setName("Test MCP Service");
        sInfo.setDescription("Test MCP service for unit tests");
        sInfo.setCode("test-mcp-service");
        content.setServiceInfo(sInfo);
        content.setDataScopes(List.of(McpServiceSnapshotMapper.toDataScopeDraft(TestFixtures.createTestDataScope())));
        content.setCustomTools(Collections.emptyList());
        content.setPrompts(Collections.emptyList());
        McpPrebuiltToolConfig prebuilt = new McpPrebuiltToolConfig();
        prebuilt.setId("pre-001");
        prebuilt.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        prebuilt.setToolType(McpToolType.SEARCH_METADATA);
        prebuilt.setEnabled(true);
        prebuilt.setConfig(new ToolConfig.SearchMetadataConfig());
        content.setPrebuiltTools(List.of(McpServiceSnapshotMapper.toPrebuiltToolDraft(prebuilt)));

        McpServiceSnapshotPO snapshotPO = new McpServiceSnapshotPO();
        snapshotPO.setId("snap-001");
        snapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPO.setVersionNumber(1);
        snapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(content));

        // 草稿：同一条 prebuilt 记录，但带 DB 审计字段（时间戳）
        McpPrebuiltToolConfig currentPrebuilt = new McpPrebuiltToolConfig();
        currentPrebuilt.setId("pre-001");
        currentPrebuilt.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        currentPrebuilt.setToolType(McpToolType.SEARCH_METADATA);
        currentPrebuilt.setEnabled(true);
        currentPrebuilt.setConfig(new ToolConfig.SearchMetadataConfig());
        currentPrebuilt.setCreatedAt(java.time.Instant.now());
        currentPrebuilt.setUpdatedAt(java.time.Instant.now());

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findById("snap-001")).thenReturn(Optional.of(snapshotPO));
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(new ToolsResult(List.of(currentPrebuilt), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isHasChanges()).isFalse();
    }

    @Test
    @DisplayName("Custom tool only differs in audit fields (no real change) - diff has no changes")
    void getDiff_CustomToolOnlyAuditFieldsDiffer_NoChanges() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        // 快照：custom tool 带 id，无时间戳（parseContent 丢弃审计字段）
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo sInfo = new McpServiceSnapshot.ServiceInfo();
        sInfo.setId("mcp-svc-001");
        sInfo.setName("Test MCP Service");
        sInfo.setDescription("Test MCP service for unit tests");
        sInfo.setCode("test-mcp-service");
        content.setServiceInfo(sInfo);
        content.setDataScopes(List.of(McpServiceSnapshotMapper.toDataScopeDraft(TestFixtures.createTestDataScope())));
        content.setPrebuiltTools(Collections.emptyList());
        content.setPrompts(Collections.emptyList());
        McpCustomTool snapshotTool = TestFixtures.createTestCustomTool();
        content.setCustomTools(List.of(McpServiceSnapshotMapper.toCustomToolDraft(snapshotTool)));

        McpServiceSnapshotPO snapshotPO = new McpServiceSnapshotPO();
        snapshotPO.setId("snap-001");
        snapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPO.setVersionNumber(1);
        snapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(content));

        // 草稿：同 name/config 的 tool，时间戳被回滚刷新
        McpCustomTool currentTool = TestFixtures.createTestCustomTool();
        currentTool.setCreatedAt(java.time.Instant.now());
        currentTool.setUpdatedAt(java.time.Instant.now());

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findById("snap-001")).thenReturn(Optional.of(snapshotPO));
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(new ToolsResult(Collections.emptyList(), List.of(currentTool)));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isHasChanges()).isFalse();
    }

    @Test
    @DisplayName("Data scope only differs in id/timestamps (no real change) - diff has no changes")
    void getDiff_DataScopeOnlyAuditFieldsDiffer_NoChanges() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo sInfo = new McpServiceSnapshot.ServiceInfo();
        sInfo.setId("mcp-svc-001");
        sInfo.setName("Test MCP Service");
        sInfo.setDescription("Test MCP service for unit tests");
        sInfo.setCode("test-mcp-service");
        content.setServiceInfo(sInfo);
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setId("sc-001");
        scope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-001");
        content.setDataScopes(List.of(McpServiceSnapshotMapper.toDataScopeDraft(scope)));
        content.setPrebuiltTools(Collections.emptyList());
        content.setCustomTools(Collections.emptyList());
        content.setPrompts(Collections.emptyList());

        McpServiceSnapshotPO snapshotPO = new McpServiceSnapshotPO();
        snapshotPO.setId("snap-001");
        snapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        snapshotPO.setVersionNumber(1);
        snapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(content));

        // 草稿：同一 scope，时间戳被回滚刷新
        McpServiceDataScope currentScope = new McpServiceDataScope();
        currentScope.setId("sc-001");
        currentScope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        currentScope.setScopeType(McpDataScopeType.DATA_SOURCE);
        currentScope.setReferenceId("ds-001");
        currentScope.setCreatedAt(java.time.Instant.now());
        currentScope.setUpdatedAt(java.time.Instant.now());

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findById("snap-001")).thenReturn(Optional.of(snapshotPO));
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(currentScope));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID))
                .thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());

        McpServiceDiffVO diff = publishService.getDiff(TestFixtures.TEST_MCP_SERVICE_ID);

        assertThat(diff.isHasChanges()).isFalse();
    }

    // ── 回滚 ──

    @Test
    @DisplayName("Rollback - target version content restored to draft and new snapshot created")
    void rollback_OverwritesDraftAndCreatesNewSnapshot() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-002");
        testServicePO.setActiveVersionNumber(2);

        // v1 快照内容（与当前草稿不同的旧配置）
        McpServiceSnapshot.SnapshotContent v1Content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo v1Info = new McpServiceSnapshot.ServiceInfo();
        v1Info.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        v1Info.setName("Test MCP Service");
        v1Info.setDescription("Legacy description from v1");
        v1Info.setCode("test-mcp-service");
        v1Content.setServiceInfo(v1Info);

        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-001");
        v1Content.setDataScopes(List.of(McpServiceSnapshotMapper.toDataScopeDraft(scope)));

        v1Content.setPrebuiltTools(Collections.emptyList());
        McpCustomTool v1Tool = TestFixtures.createTestCustomTool();
        v1Tool.setDescription("v1 tool description");
        v1Content.setCustomTools(List.of(McpServiceSnapshotMapper.toCustomToolDraft(v1Tool)));

        McpPrompt v1Prompt = TestFixtures.createTestMcpPrompt();
        v1Prompt.setDescription("v1 prompt description");
        v1Content.setPrompts(List.of(McpServiceSnapshotMapper.toPromptDraft(v1Prompt)));

        McpServiceSnapshotPO v1SnapshotPO = new McpServiceSnapshotPO();
        v1SnapshotPO.setId("snap-001");
        v1SnapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        v1SnapshotPO.setVersionNumber(1);
        v1SnapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(v1Content));

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findByServiceIdAndVersionNumber(TestFixtures.TEST_MCP_SERVICE_ID, 1)).thenReturn(Optional.of(v1SnapshotPO));
        when(snapshotDAO.findMaxVersionNumberByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(2);
        // publish 内部 buildCurrentSnapshotContent 读取（已恢复的）草稿
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());
        when(snapshotDAO.save(any(McpServiceSnapshotPO.class))).thenAnswer(inv -> {
            McpServiceSnapshotPO po = inv.getArgument(0);
            po.setId("snap-003");
            return po;
        });
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        McpServiceSnapshot snapshot = publishService.rollback(TestFixtures.TEST_MCP_SERVICE_ID, 1, "恢复");

        // 1. 草稿被 v1 内容覆盖（基础信息）
        assertThat(testServicePO.getDescription()).isEqualTo("Legacy description from v1");
        // 2. 各组件全量写回草稿
        verify(dataScopeService).saveDataScope(eq(TestFixtures.TEST_MCP_SERVICE_ID), anyList());
        verify(toolService).replaceCustomTools(eq(TestFixtures.TEST_MCP_SERVICE_ID), anyList());
        verify(promptService).replacePrompts(eq(TestFixtures.TEST_MCP_SERVICE_ID), anyList());
        // 3. 生成新快照 v3
        assertThat(snapshot.getVersionNumber()).isEqualTo(3);
        assertThat(snapshot.getReleaseNote()).isEqualTo("Rollback to v1: 恢复");
        assertThat(testServicePO.getActiveVersionNumber()).isEqualTo(3);
        assertThat(testServicePO.getActiveVersionId()).isEqualTo("snap-003");
        assertThat(testServicePO.getStatus()).isEqualTo(McpServiceStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Rollback to legacy snapshot with empty data scope - succeeds (MS019 not applied internally)")
    void rollback_ToEmptyDataScopeSnapshot_Succeeds() {
        testServicePO.setStatus(McpServiceStatus.PUBLISHED);
        testServicePO.setActiveVersionId("snap-002");
        testServicePO.setActiveVersionNumber(2);

        // v1 快照：空数据范围（MS018 规则前的历史快照）
        McpServiceSnapshot.SnapshotContent v1Content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo v1Info = new McpServiceSnapshot.ServiceInfo();
        v1Info.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        v1Info.setName("Test MCP Service");
        v1Info.setDescription("Legacy v1");
        v1Info.setCode("test-mcp-service");
        v1Content.setServiceInfo(v1Info);
        v1Content.setDataScopes(Collections.emptyList());
        v1Content.setPrebuiltTools(Collections.emptyList());
        v1Content.setCustomTools(Collections.emptyList());
        v1Content.setPrompts(Collections.emptyList());

        McpServiceSnapshotPO v1SnapshotPO = new McpServiceSnapshotPO();
        v1SnapshotPO.setId("snap-001");
        v1SnapshotPO.setServiceId(TestFixtures.TEST_MCP_SERVICE_ID);
        v1SnapshotPO.setVersionNumber(1);
        v1SnapshotPO.setSnapshotContent(com.dati.common.JsonUtils.toJson(v1Content));

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findByServiceIdAndVersionNumber(TestFixtures.TEST_MCP_SERVICE_ID, 1)).thenReturn(Optional.of(v1SnapshotPO));
        when(snapshotDAO.findMaxVersionNumberByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(2);
        // 恢复后草稿数据范围为空（v1 内容写回）
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());
        when(snapshotDAO.save(any(McpServiceSnapshotPO.class))).thenAnswer(inv -> {
            McpServiceSnapshotPO po = inv.getArgument(0);
            po.setId("snap-003");
            return po;
        });
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        McpServiceSnapshot snapshot = publishService.rollback(TestFixtures.TEST_MCP_SERVICE_ID, 1, null);

        assertThat(snapshot.getVersionNumber()).isEqualTo(3);
        assertThat(snapshot.getReleaseNote()).isEqualTo("Rollback to v1");
        assertThat(testServicePO.getActiveVersionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Rollback to missing version - throws")
    void rollback_TargetVersionNotFound_Throws() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findByServiceIdAndVersionNumber(TestFixtures.TEST_MCP_SERVICE_ID, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publishService.rollback(TestFixtures.TEST_MCP_SERVICE_ID, 99, null))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode())
                        .isEqualTo(ErrorCode.MS_SERVICE_VERSION_NOT_FOUND));
    }

    // ── 状态机前置条件 ──

    @Test
    @DisplayName("Enable DRAFT service - throws invalid status")
    void enable_DraftService_Throws() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));

        assertThatThrownBy(() -> publishService.enable(TestFixtures.TEST_MCP_SERVICE_ID))
                .isInstanceOf(DatiException.class);
    }

    @Test
    @DisplayName("Disable DRAFT service - throws invalid status")
    void disable_DraftService_Throws() {
        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));

        assertThatThrownBy(() -> publishService.disable(TestFixtures.TEST_MCP_SERVICE_ID))
                .isInstanceOf(DatiException.class);
    }

    @Test
    @DisplayName("Publish while disabled - status stays DISABLED, only snapshot updated")
    void publish_DisabledService_KeepsStatusDisabled() {
        testServicePO.setStatus(McpServiceStatus.DISABLED);
        testServicePO.setActiveVersionId("snap-001");
        testServicePO.setActiveVersionNumber(1);

        when(mcpServiceDAO.findById(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Optional.of(testServicePO));
        when(snapshotDAO.findMaxVersionNumberByServiceId(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(1);
        when(dataScopeService.getDataScope(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(List.of(TestFixtures.createTestDataScope()));
        when(toolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(new ToolsResult(Collections.emptyList(), Collections.emptyList()));
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(Collections.emptyList());
        when(snapshotDAO.save(any(McpServiceSnapshotPO.class))).thenAnswer(inv -> {
            McpServiceSnapshotPO po = inv.getArgument(0);
            po.setId("snap-002");
            return po;
        });
        when(mcpServiceDAO.save(any(McpServicePO.class))).thenAnswer(inv -> inv.getArgument(0));

        McpServiceSnapshot snapshot = publishService.publish(TestFixtures.TEST_MCP_SERVICE_ID, "update while disabled");

        assertThat(snapshot.getVersionNumber()).isEqualTo(2);
        assertThat(testServicePO.getStatus()).isEqualTo(McpServiceStatus.DISABLED);
        assertThat(testServicePO.getActiveVersionNumber()).isEqualTo(2);
    }
}
