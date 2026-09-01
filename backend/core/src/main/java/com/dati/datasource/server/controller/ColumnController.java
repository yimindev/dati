package com.dati.datasource.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.IdResponse;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import jakarta.validation.Valid;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.service.ColumnService;
import com.dati.datasource.domain.service.ColumnValueService;
import com.dati.datasource.server.assembler.ColumnAssembler;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import com.dati.datasource.server.pojo.ColumnValueListRequest;
import com.dati.datasource.server.pojo.ColumnValueVO;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/v1/data-sources/{datasourceId}/tables/{tableId}/columns")
public class ColumnController {

    private final ColumnService columnService;

    private final ColumnAssembler columnAssembler;

    private final ColumnValueService columnValueService;

    public ColumnController(ColumnService columnService, ColumnAssembler columnAssembler, ColumnValueService columnValueService) {
        this.columnService = columnService;
        this.columnAssembler = columnAssembler;
        this.columnValueService = columnValueService;
    }

    @GetMapping
    public PageResponse<ColumnInfoVO> getColumns(
            @PathVariable String tableId,
            @Valid PageReq pageReq,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return columnAssembler.toPageResponse(columnService.getColumns(pageReq, tableId, keyword));
    }

    @PutMapping("/{id}")
    public IdResponse updateColumn(@PathVariable String id, @RequestBody ColumnInfoVO columnInfoVO) {
        ColumnInfo columnInfo = columnAssembler.toColumnInfo(columnInfoVO);
        columnService.updateColumn(id, columnInfo);
        return new IdResponse(id);
    }

    @PostMapping("/sync")
    public IdResponse syncColumns(
            @PathVariable String datasourceId,
            @PathVariable String tableId,
            @RequestParam(name = "overwrite_existing", defaultValue = "false") boolean overwriteExisting) {
        try {
            columnService.syncColumns(datasourceId, tableId, overwriteExisting);
            return new IdResponse(tableId);
        } catch (SQLException e) {
            log.error("Failed to sync columns for datasource {}, table {}", datasourceId, tableId, e);
            throw new DatiException(ErrorCode.DS_SQL_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{columnId}/values/extract")
    public IdResponse extractValues(
            @PathVariable String datasourceId,
            @PathVariable String columnId,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        try {
            columnValueService.extractValues(datasourceId, columnId, overwrite);
            return new IdResponse(columnId);
        } catch (SQLException e) {
            log.error("Failed to extract values for column {}", columnId, e);
            throw new DatiException(ErrorCode.DS_SQL_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{columnId}/values")
    public PageResponse<ColumnValueVO> getValues(
            @PathVariable String columnId,
            @Valid PageReq pageReq,
            @RequestParam(required = false) String keyword) {
        return PageResponse.of(
                columnValueService.getValues(columnId, pageReq, keyword)
                        .map(item -> {
                            ColumnValueVO vo = new ColumnValueVO();
                            vo.setId(item.getId());
                            vo.setValue(item.getValue());
                            vo.setSynonyms(item.getSynonyms());
                            return vo;
                        })
        );
    }

    @PutMapping("/{columnId}/values")
    public IdResponse saveValues(
            @PathVariable String datasourceId,
            @PathVariable String columnId,
            @RequestBody ColumnValueListRequest request) {
        List<ColumnValueService.ValueItem> items = request.getValues().stream().map(vo -> {
            ColumnValueService.ValueItem item = new ColumnValueService.ValueItem();
            item.setId(vo.getId());
            item.setValue(vo.getValue());
            item.setSynonyms(vo.getSynonyms());
            return item;
        }).toList();
        columnValueService.saveValues(columnId, items, request.getDeletedIds());
        return new IdResponse(columnId);
    }

}
