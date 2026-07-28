package com.dati.datasource.repository.mapper;

import com.dati.base.EncryptionUtils;
import com.dati.base.MapperUtils;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.repository.po.DataSourcePO;

public class DSMapper {
    
    public static DataSourcePO toDataSourcePO(DataSource dataSource) {
        DataSourcePO dataSourcePO = new DataSourcePO();
        copyProperties(dataSource, dataSourcePO);
        return dataSourcePO;
    }

    private static void copyProperties(DataSource source, DataSourcePO target) {
        MapperUtils.copyBaseResourceInfo(source, target);
        target.setType(source.getType());
        target.setJdbcUrl(source.getJdbcUrl());
        target.setUserName(source.getUsername());
        target.setEncryptedPassword(EncryptionUtils.encrypt(source.getPassword()));
    }

    public static DataSource toDataSource(DataSourcePO dataSourcePO) {
        DataSource dataSource = new DataSource();
        MapperUtils.copyBaseResourceInfo(dataSourcePO, dataSource);
        dataSource.setType(dataSourcePO.getType());
        dataSource.setJdbcUrl(dataSourcePO.getJdbcUrl());
        dataSource.setUsername(dataSourcePO.getUserName());
        dataSource.setPassword(EncryptionUtils.decrypt(dataSourcePO.getEncryptedPassword()));
        dataSource.setDefaultSchema(dataSourcePO.getDefaultSchema());
        return dataSource;
    }

}
