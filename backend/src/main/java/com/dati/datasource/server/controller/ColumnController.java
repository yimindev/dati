package com.dati.datasource.server.controller;

import com.dati.base.exception.DciException;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.service.ColumnService;
import com.dati.datasource.server.assembler.ColumnAssembler;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/v1/data-sources/{datasourceId}/tables/{tableId}/columns")
public class ColumnController {

    private final ColumnService columnService;

    private final ColumnAssembler columnAssembler;

    public ColumnController(ColumnService columnService, ColumnAssembler columnAssembler) {
        this.columnService = columnService;
        this.columnAssembler = columnAssembler;
    }

    @GetMapping
    public PageResponse<ColumnInfoVO> getColumns(
            @PathVariable String datasourceId,
            @PathVariable String tableId,
            PageReq pageReq,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return PageResponse.of(columnService.getColumns(pageReq, datasourceId, tableId, keyword).map(columnAssembler::toColumnInfoVO));
    }

    @PutMapping("/{id}")
    public IdResponse updateColumn(@PathVariable String datasourceId, @PathVariable String tableId, @PathVariable String id, @RequestBody ColumnInfoVO columnInfoVO) {
        ColumnInfo columnInfo = columnAssembler.toColumnInfo(columnInfoVO);
        columnService.updateColumn(id, columnInfo);
        return new IdResponse(id);
    }

    @PostMapping("/sync")
    public IdResponse syncColumns(@PathVariable String datasourceId, @PathVariable String tableId) {
        try {
            columnService.syncColumns(datasourceId, tableId);
            return new IdResponse(tableId);
        } catch (SQLException e) {
            log.error("Failed to sync columns for datasource {}, table {}", datasourceId, tableId, e);
            throw new DciException("SQL Error: " + e.getMessage());
        }
    }

}
