package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.TableInfoPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableInfoDAO extends JpaRepository<TableInfoPO, String> {
    Page<TableInfoPO> findByDataSourceId(String dataSourceId, Pageable pageable);
}
