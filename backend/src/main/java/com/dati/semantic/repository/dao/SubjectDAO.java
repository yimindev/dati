package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectDAO extends JpaRepository<SubjectPO, String> {
    Page<SubjectPO> findByDatasourceId(String datasourceId, Pageable pageable);
}
