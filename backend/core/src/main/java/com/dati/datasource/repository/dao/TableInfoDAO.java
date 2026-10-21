package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.TableInfoPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableInfoDAO extends JpaRepository<TableInfoPO, String> {
    Page<TableInfoPO> findByDataSourceId(String dataSourceId, Pageable pageable);

    @Query("""
            SELECT t FROM TableInfoPO t
            WHERE t.dataSourceId = :dataSourceId
              AND (locate(lower(:keyword), lower(t.name)) > 0
                   OR locate(lower(:keyword), lower(cast(t.aliases as String))) > 0)
            """)
    Page<TableInfoPO> findByDataSourceIdAndKeyword(@Param("dataSourceId") String dataSourceId,
                                                   @Param("keyword") String keyword,
                                                   Pageable pageable);
    List<TableInfoPO> findByDataSourceId(String dataSourceId);
    List<TableInfoPO> findByDataSourceIdAndSchema(String dataSourceId, String schema);
    Optional<TableInfoPO> findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(
            String dataSourceId, String schema, String name);
}
