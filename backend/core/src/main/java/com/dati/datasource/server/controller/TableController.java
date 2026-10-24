package com.dati.datasource.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import jakarta.validation.Valid;
import com.dati.datasource.domain.service.TableService;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.datasource.server.pojo.AddedTableVO;
import com.dati.datasource.server.pojo.BatchAddTablesResponse;
import com.dati.datasource.server.pojo.TableInfoVO;
import com.dati.datasource.server.pojo.request.UpdateTableRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/data-sources/{datasourceId}")
public class TableController {

    private final TableService tableService;

    private final TableAssembler tableAssembler;

    public TableController(TableService tableService, TableAssembler tableAssembler) {
        this.tableService = tableService;
        this.tableAssembler = tableAssembler;
    }

    @GetMapping("/tables")
    public PageResponse<TableInfoVO> getTables(@PathVariable String datasourceId, @Valid PageReq pageReq, String keyword) {
        return tableAssembler.toPageResponse(tableService.getTables(pageReq, datasourceId, keyword));
    }

    @GetMapping("/tables/added-names")
    public List<AddedTableVO> getAddedTableNames(@PathVariable String datasourceId) {
        return tableAssembler.toAddedTables(tableService.getAddedTables(datasourceId));
    }

    @PostMapping("/tables/batch")
    public BatchAddTablesResponse batchAddTables(@PathVariable String datasourceId,
                                                 @Valid @RequestBody List<@Valid AddTableRequest> tables) {
        List<String> ids = tableService.batchAddTables(datasourceId, tables);
        return new BatchAddTablesResponse(ids.size(), ids);
    }

    @DeleteMapping("/tables/{tableId}")
    public IdResponse deleteTable(@PathVariable String datasourceId, @PathVariable String tableId) {
        tableService.deleteTable(datasourceId, tableId);
        return new IdResponse(tableId);
    }

    @PutMapping("/tables/{tableId}")
    public IdResponse updateTable(
            @PathVariable String datasourceId,
            @PathVariable String tableId,
            @Valid @RequestBody UpdateTableRequest request) {
        var tableInfo = tableAssembler.toTableInfo(request);
        tableAssembler.fillUpdateUserFromRequest(tableInfo);
        tableService.updateTable(datasourceId, tableId, tableInfo);
        return new IdResponse(tableId);
    }

}
