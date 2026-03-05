package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import org.springframework.stereotype.Component;

@Component
public class ColumnAssembler extends BaseAssembler {

    public ColumnInfoVO toColumnInfoVO(ColumnInfo columnInfo) {
        ColumnInfoVO columnInfoVO = new ColumnInfoVO();
        super.copyBaseInfo(columnInfo, columnInfoVO);
        columnInfoVO.setTableId(columnInfo.getTableId());
        columnInfoVO.setColumnName(columnInfo.getColumnName());
        columnInfoVO.setColumnType(columnInfo.getColumnType());
        columnInfoVO.setComment(columnInfo.getComment());
        return columnInfoVO;
    }

    public ColumnInfo toColumnInfo(ColumnInfoVO columnInfoVO) {
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setId(columnInfoVO.getId());
        columnInfo.setName(columnInfoVO.getName());
        columnInfo.setDescription(columnInfoVO.getDescription());
        columnInfo.setCreatedBy(columnInfoVO.getCreatedBy());
        columnInfo.setCreatedAt(columnInfoVO.getCreatedAt());
        columnInfo.setUpdatedBy(columnInfoVO.getUpdatedBy());
        columnInfo.setUpdatedAt(columnInfoVO.getUpdatedAt());
        columnInfo.setTableId(columnInfoVO.getTableId());
        columnInfo.setColumnName(columnInfoVO.getColumnName());
        columnInfo.setColumnType(columnInfoVO.getColumnType());
        columnInfo.setComment(columnInfoVO.getComment());
        return columnInfo;
    }

}
