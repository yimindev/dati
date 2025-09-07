package com.dataconnai.datasource.repository.dao;

import com.dataconnai.datasource.repository.po.DataSourcePO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceDAO extends JpaRepository<DataSourcePO, String> {

}
