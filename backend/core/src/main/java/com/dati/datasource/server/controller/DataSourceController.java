package com.dati.datasource.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.server.assembler.DSAssembler;
import com.dati.datasource.server.pojo.DatasourceVO;
import com.dati.datasource.server.pojo.request.UpdateDataSourceRequest;
import com.dati.db.Table;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping
    public IdResponse addDataSource(@Valid @RequestBody DataSource dataSource) {
        dsAssembler.fillUsersFromRequest(dataSource);
        return new IdResponse(dataSourceService.addDataSource(dataSource));
    }

    @PutMapping("/{id}")
    public IdResponse updateDataSource(@PathVariable String id, @Valid @RequestBody UpdateDataSourceRequest request) {
        DataSource dataSource = new DataSource();
        dataSource.setName(request.getName());
        dataSource.setDescription(request.getDescription());
        dataSource.setJdbcUrl(request.getJdbcUrl());
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        dataSource.setType(request.getType());
        dataSource.setDefaultSchema(request.getDefaultSchema());
        dsAssembler.fillUpdateUserFromRequest(dataSource);
        dataSourceService.updateDataSource(id, dataSource);
        return new IdResponse(id);
    }

    @DeleteMapping("/{id}")
    public IdResponse deleteDataSource(@PathVariable String id) {
        dataSourceService.deleteDataSource(id);
        return new IdResponse(id);
    }

    @GetMapping
    public PageResponse<DatasourceVO> listDataSources(@Valid PageReq pageReq, @RequestParam(name = "keyword", required = false) String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        return dsAssembler.toPageResponse(
                dataSourceService.listDataSources(keyword, pageReq.toPageRequest().withSort(sortBy)));
    }

    @GetMapping("/{id}")
    public DatasourceVO getDataSource(@PathVariable String id) {
        return dsAssembler.toDatasourceVO(
            dataSourceService.getDataSource(id)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, id)));
    }

    @GetMapping("/{id}/schemas")
    public List<String> getSchemas(@PathVariable String id) {
        try {
            return dataSourceService.getSchemas(id);
        } catch (SQLException e) {
            log.error("Failed to get schemas for datasource {}", id, e);
            throw new DatiException(ErrorCode.DS_SQL_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables")
    public List<Table> getTables(@PathVariable String id, @PathVariable String schema) {
        try {
            return dataSourceService.getTables(id, schema);
        } catch (SQLException e) {
            log.error("Failed to get tables for datasource {}", id, e);
            throw new DatiException(ErrorCode.DS_SQL_ERROR, e.getMessage());
        }
    }
}
