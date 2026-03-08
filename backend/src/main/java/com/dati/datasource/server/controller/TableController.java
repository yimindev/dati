package com.dati.datasource.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
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
    public PageResponse<TableInfoVO> getTables(@PathVariable String datasourceId, PageReq pageReq, String keyword) {
        return PageResponse.of(tableService.getTables(pageReq, datasourceId, keyword).map(tableAssembler::toTableInfoVO));
    }

    @GetMapping("/tables/added-names")
    public List<String> getAddedTableNames(@PathVariable String datasourceId) {
        return tableService.getAddedTableNames(datasourceId);
    }

    @PostMapping("/tables/batch")
    public IdResponse batchAddTables(@PathVariable String datasourceId, @RequestBody List<AddTableRequest> tables) {
        List<String> ids = tableService.batchAddTables(datasourceId, tables);
        return new IdResponse(String.valueOf(ids.size()));
    }

}
