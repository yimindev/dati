package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ColumnAssembler extends BaseAssembler {

    private ColumnInfoVO mapFields(ColumnInfo columnInfo) {
        ColumnInfoVO columnInfoVO = new ColumnInfoVO();
        super.copyBaseInfo(columnInfo, columnInfoVO);
        columnInfoVO.setTableId(columnInfo.getTableId());
        columnInfoVO.setColumnType(columnInfo.getColumnType());
        columnInfoVO.setAliases(columnInfo.getAliases() != null ? columnInfo.getAliases() : new ArrayList<>());
        columnInfoVO.setExtractValueEnabled(Boolean.TRUE.equals(columnInfo.getExtractValueEnabled()));
        return columnInfoVO;
    }

    public ColumnInfoVO toColumnInfoVO(ColumnInfo columnInfo) {
        return mapFields(columnInfo);
    }

    public PageResponse<ColumnInfoVO> toPageResponse(Page<ColumnInfo> page) {
        List<ColumnInfoVO> vos = page.getContent().stream()
                .map(this::mapFields)
                .collect(Collectors.toList());
        super.fillUserInfo(vos);
        return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
    }

    public ColumnInfo toColumnInfo(ColumnInfoVO columnInfoVO) {
        ColumnInfo columnInfo = new ColumnInfo();
        super.copyBaseInfo(columnInfoVO, columnInfo);
        columnInfo.setTableId(columnInfoVO.getTableId());
        columnInfo.setColumnType(columnInfoVO.getColumnType());
        columnInfo.setAliases(columnInfoVO.getAliases());
        columnInfo.setExtractValueEnabled(columnInfoVO.getExtractValueEnabled());
        return columnInfo;
    }

}
