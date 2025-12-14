package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.DataSourcePO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceDAO extends JpaRepository<DataSourcePO, String> {

    Page<DataSourcePO> findAllByNameContainingOrId(String name, String id, Pageable pageable);
}
