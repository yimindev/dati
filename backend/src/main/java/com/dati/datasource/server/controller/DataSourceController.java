package com.dati.datasource.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.server.assembler.DSAssembler;
import com.dati.datasource.server.pojo.DatasourceVO;
import com.dati.datasource.server.pojo.SqlExecuteRequest;
import com.dati.db.Column;
import com.dati.db.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final JdbcMetaService jdbcMetaService;

    private final DSAssembler dsAssembler;

    public DataSourceController(DataSourceService dataSourceService, JdbcMetaService jdbcMetaService, DSAssembler dsAssembler) {
        this.dataSourceService = dataSourceService;
        this.jdbcMetaService = jdbcMetaService;
        this.dsAssembler = dsAssembler;
    }

    @PostMapping("/test-connection")
    public boolean testConnection(@RequestBody DataSource dataSource) {
        return dataSourceService.testConnection(DSAssembler.toJdbcConnector(dataSource));
    }

    @PostMapping
    public IdResponse addDataSource(@RequestBody DataSource dataSource) {
        dsAssembler.fillUsersFromRequest(dataSource);
        return new IdResponse(dataSourceService.addDataSource(dataSource));
    }

    @PutMapping("/{id}")
    public IdResponse updateDataSource(@PathVariable String id, @RequestBody DataSource dataSource) {
        dsAssembler.fillUpdateUserFromRequest(dataSource);
        dataSourceService.updateDataSource(id, dataSource);
        return new IdResponse(dataSource.getId());
    }

    @DeleteMapping("/{id}")
    public IdResponse deleteDataSource(@PathVariable String id) {
        dataSourceService.deleteDataSource(id);
        return new IdResponse(id);
    }

    @GetMapping
    public PageResponse<DatasourceVO> listDataSources(PageReq pageReq, @RequestParam(name = "keyword", required = false) String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        Page<DatasourceVO> datasourceVOPage = dataSourceService.listDataSources(keyword, pageReq.toPageRequest().withSort(sortBy))
                .map(dsAssembler::toDatasourceVO);
        return PageResponse.of(datasourceVOPage);
    }


    @GetMapping("/{id}/schemas")
    public List<String> getSchemas(@PathVariable String id, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return jdbcMetaService.getSchemas(id, catalog);
        } catch (SQLException e) {
            log.error("Failed to get schemas for datasource {}", id, e);
            throw new DatiException("SQL Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables")
    public List<Table> getTables(@PathVariable String id, @PathVariable String schema, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return jdbcMetaService.getTables(id, catalog, schema);
        } catch (SQLException e) {
            log.error("Failed to get tables for datasource {}", id, e);
            throw new DatiException("SQL Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables/{table}/columns")
    public List<Column> getColumns(@PathVariable String id, @PathVariable String schema, @PathVariable String table, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return jdbcMetaService.getColumns(id, catalog, schema, table);
        } catch (SQLException e) {
            log.error("Failed to get columns for datasource {}", id, e);
            throw new DatiException("SQL Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/execute-sql")
    public List<Map<String, Object>> executeSql(@PathVariable String id, @RequestBody SqlExecuteRequest sqlExecuteRequest) {
        try {
            return jdbcMetaService.executeSql(id, sqlExecuteRequest.sql());
        } catch (SQLException e) {
            log.error("Failed to execute SQL for datasource {}", id, e);
            throw new DatiException("SQL Error: " + e.getMessage());
        }
    }
}
