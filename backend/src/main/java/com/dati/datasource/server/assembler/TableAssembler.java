package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.server.pojo.TableInfoVO;
import org.springframework.stereotype.Component;

@Component
public class TableAssembler extends BaseAssembler {

    public TableInfoVO toTableInfoVO(TableInfo tableInfo) {
        TableInfoVO tableInfoVO = new TableInfoVO();
        super.copyBaseInfo(tableInfo, tableInfoVO);
        tableInfoVO.setDatasourceId(tableInfo.getDatasourceId());
        tableInfoVO.setSchema(tableInfo.getSchema());
        tableInfoVO.setAliases(tableInfo.getAliases());
        return tableInfoVO;
    }

    public TableInfo toTableInfo(TableInfoVO vo) {
        TableInfo tableInfo = new TableInfo();
        super.copyBaseInfo(vo, tableInfo);
        tableInfo.setDatasourceId(vo.getDatasourceId());
        tableInfo.setSchema(vo.getSchema());
        tableInfo.setAliases(vo.getAliases());
        tableInfo.setDescription(vo.getDescription());
        return tableInfo;
    }

}
