package com.dati.datasource.server.controller;

import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.domain.service.TableService;
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

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("/tables")
    public PageResponse<TableInfo> getTables(PageReq pageReq, @PathVariable("datasourceId") String datasourceId) {
        return PageResponse.of(tableService.getTables(pageReq, datasourceId));
    }

}
