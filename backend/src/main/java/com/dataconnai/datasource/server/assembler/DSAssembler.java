package com.dataconnai.datasource.server.assembler;

import com.dataconnai.base.BaseAssembler;
import com.dataconnai.db.JdbcConnector;
import com.dataconnai.datasource.domain.model.DataSource;
import com.dataconnai.datasource.server.pojo.DatasourceVO;
import org.springframework.stereotype.Component;

import java.util.List;

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
        return datasourceVO;
    }

    public List<DatasourceVO> toDatasourceVOList(List<DataSource> dataSources) {
        List<DatasourceVO> voList = dataSources.stream().map(this::toDatasourceVO).toList();
        super.fillUserInfo(voList);
        return voList;
    }

}
