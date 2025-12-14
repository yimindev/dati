package com.dati.datasource.server.controller;

import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.service.TableService;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.TableInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
