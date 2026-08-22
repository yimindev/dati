package com.dati.datasource.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.EncryptionUtils;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.DataSourceDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.DSMapper;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.JdbcUtils;
import com.dati.db.Table;
import com.dati.permission.domain.service.PermissionService;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import com.dati.semantic.domain.service.SemanticIndexService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataSourceService {

    private final DataSourceDAO dataSourceDAO;
    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final SemanticIndexService semanticIndexService;
    private final JdbcMetaService jdbcMetaService;
    private final PermissionService permissionService;
    private final UserGroupService userGroupService;

    public DataSourceService(DataSourceDAO dataSourceDAO, TableInfoDAO tableInfoDAO,
                             ColumnInfoDAO columnInfoDAO, SemanticIndexService semanticIndexService,
                             JdbcMetaService jdbcMetaService, PermissionService permissionService,
                             UserGroupService userGroupService) {
        this.dataSourceDAO = dataSourceDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.semanticIndexService = semanticIndexService;
        this.jdbcMetaService = jdbcMetaService;
        this.permissionService = permissionService;
        this.userGroupService = userGroupService;
    }

    public boolean testConnection(JdbcConnector jdbcConnector) {
        return JdbcUtils.testConnection(jdbcConnector.jdbcUrl(), jdbcConnector.username(), jdbcConnector.password());
    }

    /**
     * 新增数据源：忽略客户端传入的 defaultSchema，在持久化前探测真实 schema。
     * 探测失败（连接异常、不支持的类型、探测结果为空）均转为 DatiException，且不落库。
     */
    public String addDataSource(DataSource dataSource) {
        String schema = detectDefaultSchema(dataSource);
        DataSourcePO dataSourcePO = DSMapper.toDataSourcePO(dataSource);
        dataSourcePO.setDefaultSchema(schema);
        dataSourcePO = dataSourceDAO.save(dataSourcePO);
        return dataSourcePO.getId();
    }

    /**
     * 更新数据源：仅当 jdbcUrl/username/password/type 任一实际变化时才重新探测 defaultSchema。
     * 探测失败时不修改、不保存任何数据；探测成功后与其它字段一并单次保存，保存成功后再用旧连接信息关闭旧连接池。
     * 不加 @Transactional：探测涉及外部网络 I/O，不应包裹在元数据库事务中。
     */
    public void updateDataSource(String id, DataSource dataSource) {
        DataSourcePO po = dataSourceDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.DATA_SOURCE, id, Permission.EDIT, po.getCreatedBy());

        DataSource current = DSMapper.toDataSource(po);
        JdbcConnector oldConnector = new JdbcConnector(current);

        String newJdbcUrl = dataSource.getJdbcUrl() != null ? dataSource.getJdbcUrl() : current.getJdbcUrl();
        String newUsername = dataSource.getUsername() != null ? dataSource.getUsername() : current.getUsername();
        String newPassword = dataSource.getPassword() != null ? dataSource.getPassword() : current.getPassword();
        DbType newType = dataSource.getType() != null ? dataSource.getType() : current.getType();

        boolean connectionChanged = !newJdbcUrl.equals(current.getJdbcUrl())
                || !newUsername.equals(current.getUsername())
                || !newPassword.equals(current.getPassword())
                || newType != current.getType();

        String schema = null;
        if (connectionChanged) {
            DataSource candidate = new DataSource();
            candidate.setJdbcUrl(newJdbcUrl);
            candidate.setUsername(newUsername);
            candidate.setPassword(newPassword);
            candidate.setType(newType);
            // 探测失败时直接抛出，不修改 po、不保存
            schema = detectDefaultSchema(candidate);
        }

        if (dataSource.getName() != null) {
            po.setName(dataSource.getName());
        }
        if (dataSource.getDescription() != null) {
            po.setDescription(dataSource.getDescription());
        }
        if (dataSource.getJdbcUrl() != null) {
            po.setJdbcUrl(dataSource.getJdbcUrl());
        }
        if (dataSource.getType() != null) {
            po.setType(dataSource.getType());
        }
        if (dataSource.getUsername() != null) {
            po.setUserName(dataSource.getUsername());
        }
        if (dataSource.getPassword() != null) {
            po.setEncryptedPassword(EncryptionUtils.encrypt(dataSource.getPassword()));
        }
        if (dataSource.getUpdatedBy() != null) {
            po.setUpdatedBy(dataSource.getUpdatedBy());
        }
        if (connectionChanged) {
            po.setDefaultSchema(schema);
        }

        dataSourceDAO.save(po);

        if (connectionChanged) {
            // 保存成功后，用变更前的连接信息关闭旧连接池
            HikariPoolManager.close(oldConnector);
        }
    }

    /** 探测数据源的真实 defaultSchema，失败（连接异常、不支持类型、结果为空）时抛出明确的业务异常。 */
    private String detectDefaultSchema(DataSource dataSource) {
        JdbcConnector connector = new JdbcConnector(dataSource);
        String schema;
        try {
            schema = jdbcMetaService.resolveCurrentSchema(connector, dataSource.getType());
        } catch (SQLException e) {
            throw new DatiException(ErrorCode.DS_CONNECTION_FAILED, e.getMessage());
        }
        if (schema == null || schema.isBlank()) {
            throw new DatiException(ErrorCode.DS_SCHEMA_DETECTION_FAILED, dataSource.getJdbcUrl());
        }
        return schema;
    }

    @Transactional
    public void deleteDataSource(String id) {
        Optional<DataSourcePO> dataSourcePOOptional = dataSourceDAO.findById(id);
        if (dataSourcePOOptional.isEmpty()) {
            throw new DatiException(ErrorCode.DS_NOT_FOUND, id);
        }
        DataSourcePO po = dataSourcePOOptional.get();
        permissionService.requireCurrentUser(ResourceType.DATA_SOURCE, id, Permission.EDIT, po.getCreatedBy());
        JdbcConnector jdbcConnector = new JdbcConnector(DSMapper.toDataSource(po));
        HikariPoolManager.close(jdbcConnector);

        List<String> tableIds = tableInfoDAO.findByDataSourceId(id)
                .stream().map(TableInfoPO::getId).toList();
        columnInfoDAO.deleteByTableIdIn(tableIds);
        tableInfoDAO.deleteAllById(tableIds);
        semanticIndexService.deleteByEntityTableIds(tableIds);

        dataSourceDAO.deleteById(id);
    }

    public Map<String, String> getDataSourceNameMap(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return dataSourceDAO.findAllById(ids).stream()
                .collect(Collectors.toMap(DataSourcePO::getId, DataSourcePO::getName));
    }

    public Page<DataSource> listDataSources(String keyword, Pageable pageable) {
        User user = RequestContext.getUser();
        if (permissionService.isAdmin(user.getName())) {
            if (StringUtils.isEmpty(keyword)) {
                return dataSourceDAO.findAll(pageable).map(DSMapper::toDataSource);
            }
            return dataSourceDAO.findAllByNameContainingOrId(keyword, keyword, pageable)
                    .map(DSMapper::toDataSource);
        }
        if (StringUtils.isEmpty(keyword)) {
            return dataSourceDAO.findAllAccessible(user.getId(),
                            userGroupService.groupIdsOf(user.getId()), pageable)
                    .map(DSMapper::toDataSource);
        }
        return dataSourceDAO.findByNameContainingOrIdAndAccessible(keyword, user.getId(),
                        userGroupService.groupIdsOf(user.getId()), pageable)
                .map(DSMapper::toDataSource);
    }

    public Optional<DataSource> getDataSource(String id) {
        return dataSourceDAO.findById(id)
                .map(po -> {
                    permissionService.requireCurrentUser(ResourceType.DATA_SOURCE, id, Permission.VIEW, po.getCreatedBy());
                    return DSMapper.toDataSource(po);
                });
    }

    /**
     * Internal data source loading (used by MCP tool execution and internal components;
     * bypasses direct user-level data source permission checks).
     */
    public Optional<DataSource> getDataSourceInternal(String id) {
        return dataSourceDAO.findById(id).map(DSMapper::toDataSource);
    }

    public List<String> getSchemas(String id, @Nullable String catalog) throws SQLException {
        permissionService.requireDataSource(id, Permission.VIEW);
        return jdbcMetaService.getSchemas(id, catalog);
    }

    public List<Table> getTables(String id, @Nullable String catalog, String schema) throws SQLException {
        permissionService.requireDataSource(id, Permission.VIEW);
        return jdbcMetaService.getTables(id, catalog, schema);
    }

    public record DsBrief(String name, DbType dbType, String defaultSchema, String description) {}

    /** Bulk lookup for lightweight data source info, used by SEARCH_METADATA grouping. */
    public Map<String, DsBrief> getDataSourceBriefs(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return dataSourceDAO.findAllById(ids).stream()
                .collect(Collectors.toMap(DataSourcePO::getId,
                        po -> new DsBrief(po.getName(), po.getType(),
                                po.getDefaultSchema(), po.getDescription())));
    }
}
