package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.ColumnInfoPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColumnInfoDAO extends JpaRepository<ColumnInfoPO, String> {

    Page<ColumnInfoPO> findByTableId(String tableId, Pageable pageable);

    Page<ColumnInfoPO> findByTableIdAndNameContaining(String tableId, String columnName, Pageable pageable);

    void deleteByTableId(String tableId);

    void deleteByTableIdIn(List<String> tableIds);

}
