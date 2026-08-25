package com.dati.datasource.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import jakarta.validation.Valid;
import com.dati.datasource.domain.service.TableService;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.datasource.server.pojo.TableInfoVO;
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
    public List<String> getAddedTableNames(@PathVariable String datasourceId) {
        return tableService.getAddedTableNames(datasourceId);
    }

    @PostMapping("/tables/batch")
    public IdResponse batchAddTables(@PathVariable String datasourceId,
                                     @Valid @RequestBody List<@Valid AddTableRequest> tables) {
        List<String> ids = tableService.batchAddTables(datasourceId, tables);
        return new IdResponse(String.valueOf(ids.size()));
    }

    @DeleteMapping("/tables/{tableId}")
    public IdResponse deleteTable(@PathVariable String datasourceId, @PathVariable String tableId) {
        tableService.deleteTable(tableId);
        return new IdResponse(tableId);
    }

    @PutMapping("/tables/{tableId}")
    public IdResponse updateTable(
            @PathVariable String datasourceId,
            @PathVariable String tableId,
            @RequestBody TableInfoVO tableInfoVO) {
        tableService.updateTable(tableId, tableAssembler.toTableInfo(tableInfoVO));
        return new IdResponse(tableId);
    }

}
