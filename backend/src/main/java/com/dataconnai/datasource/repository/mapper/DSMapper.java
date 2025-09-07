package com.dataconnai.datasource.repository.mapper;

import com.dataconnai.base.EncryptionUtils;
import com.dataconnai.base.MapperUtils;
import com.dataconnai.datasource.domain.model.DataSource;
import com.dataconnai.datasource.repository.po.DataSourcePO;

public class DSMapper {
    
    public static DataSourcePO toDataSourcePO(DataSource dataSource) {
        DataSourcePO dataSourcePO = new DataSourcePO();
        MapperUtils.copyBaseInfo(dataSource, dataSourcePO);
        dataSourcePO.setType(dataSource.getType());
        dataSourcePO.setJdbcUrl(dataSource.getJdbcUrl());
        dataSourcePO.setUserName(dataSource.getUsername());
        dataSourcePO.setEncryptedPassword(EncryptionUtils.encrypt(dataSource.getPassword()));
        return dataSourcePO;
    }

    public static DataSource toDataSource(DataSourcePO dataSourcePO) {
        DataSource dataSource = new DataSource();
        MapperUtils.copyBaseInfo(dataSourcePO, dataSource);
        dataSource.setType(dataSourcePO.getType());
        dataSource.setJdbcUrl(dataSourcePO.getJdbcUrl());
        dataSource.setUsername(dataSourcePO.getUserName());
        dataSource.setPassword(EncryptionUtils.decrypt(dataSourcePO.getEncryptedPassword()));
        return dataSource;
    }

}
