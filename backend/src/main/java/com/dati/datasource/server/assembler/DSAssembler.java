package com.dati.datasource.server.assembler;

import com.dati.base.BaseAssembler;
import com.dati.db.JdbcConnector;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.server.pojo.DatasourceVO;
import org.springframework.stereotype.Component;

@Component
public class DSAssembler extends BaseAssembler {

    public static JdbcConnector toJdbcConnector(DataSource dataSource) {
        return new JdbcConnector(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    public DatasourceVO toDatasourceVO(DataSource dataSource) {
        DatasourceVO datasourceVO = new DatasourceVO();
        super.copyBaseInfo(dataSource, datasourceVO);
        datasourceVO.setJdbcUrl(dataSource.getJdbcUrl());
        datasourceVO.setUsername(dataSource.getUsername());
        datasourceVO.setType(dataSource.getType());
        datasourceVO.setDefaultSchema(dataSource.getDefaultSchema());
        return datasourceVO;
    }

}
