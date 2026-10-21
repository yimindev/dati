package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.ColumnInfoPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ColumnInfoDAO extends JpaRepository<ColumnInfoPO, String> {

    Page<ColumnInfoPO> findByTableId(String tableId, Pageable pageable);

    List<ColumnInfoPO> findByTableId(String tableId);

    @Query("""
            SELECT c FROM ColumnInfoPO c
            WHERE c.tableId = :tableId
              AND (locate(lower(:keyword), lower(c.name)) > 0
                   OR locate(lower(:keyword), lower(cast(c.aliases as String))) > 0)
            """)
    Page<ColumnInfoPO> findByTableIdAndKeyword(@Param("tableId") String tableId,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    void deleteByTableId(String tableId);

    void deleteByTableIdIn(List<String> tableIds);

}
