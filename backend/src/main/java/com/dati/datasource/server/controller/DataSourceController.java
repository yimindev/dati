package com.dati.datasource.server.controller;

import com.dati.base.exception.DciException;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.server.assembler.DSAssembler;
import com.dati.datasource.server.pojo.DatasourceVO;
import com.dati.datasource.server.pojo.SqlExecuteRequest;
import com.dati.db.Column;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    private final DSAssembler dsAssembler;

    public DataSourceController(DataSourceService dataSourceService, DSAssembler dsAssembler) {
        this.dataSourceService = dataSourceService;
        this.dsAssembler = dsAssembler;
    }

    @PostMapping("/test-connection")
    public boolean testConnection(@RequestBody DataSource dataSource) {
        return dataSourceService.testConnection(DSAssembler.toJdbcConnector(dataSource));
    }

    @PostMapping("")
    public IdResponse addDataSource(@RequestBody DataSource dataSource) {
        dsAssembler.fillUsersFromRequest(dataSource);
        return new IdResponse(dataSourceService.addDataSource(dataSource));
    }

    @PutMapping("/{id}")
    public IdResponse updateDataSource(@PathVariable("id") String id, @RequestBody DataSource dataSource) {
        dsAssembler.fillUpdateUserFromRequest(dataSource);
        dataSourceService.updateDataSource(id, dataSource);
        return new IdResponse(dataSource.getId());
    }

    @DeleteMapping("/{id}")
    public IdResponse deleteDataSource(@PathVariable("id") String id) {
        dataSourceService.deleteDataSource(id);
        return new IdResponse(id);
    }

    @GetMapping("")
    public PageResponse<DatasourceVO> listDataSources(@RequestParam(name = "page", defaultValue = "1")int page,  @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<DatasourceVO> dataSourcePageResponse = new PageResponse<>();
        dataSourcePageResponse.setData(dsAssembler.toDatasourceVOList(dataSourceService.listDataSources()));
        dataSourcePageResponse.setTotal(dataSourceService.listDataSources().size());
        return dataSourcePageResponse;
    }

    @GetMapping("/{id}/catalogs")
    public List<String> getCatalogs(@PathVariable("id") String id) {
        try {
            return dataSourceService.getCatalogs(id);
        } catch (SQLException e) {
            log.error("Failed to get catalogs for datasource {}", id, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas")
    public List<String> getSchemas(@PathVariable("id") String id, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return dataSourceService.getSchemas(id, catalog);
        } catch (SQLException e) {
            log.error("Failed to get schemas for datasource {}", id, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables")
    public List<String> getTables(@PathVariable("id") String id, @PathVariable("schema") String schema, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return dataSourceService.getTables(id, catalog, schema);
        } catch (SQLException e) {
            log.error("Failed to get tables for datasource {}", id, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables/{table}/columns")
    public List<Column> getColumns(@PathVariable("id") String id, @PathVariable("schema") String schema, @PathVariable("table") String table, @RequestParam(name = "catalog", required = false) String catalog) {
        try {
            return dataSourceService.getColumns(id, catalog, schema, table);
        } catch (SQLException e) {
            log.error("Failed to get columns for datasource {}", id, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/execute-sql")
    public List<Map<String, Object>> executeSql(@PathVariable("id") String id, @RequestBody SqlExecuteRequest sqlExecuteRequest) {
        try {
            return dataSourceService.executeSql(id, sqlExecuteRequest.sql());
        } catch (SQLException e) {
            log.error("Failed to get columns for datasource {}", id, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }
}
