package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectDAO extends JpaRepository<SubjectPO, String> {
    List<SubjectPO> findByDatasourceId(String datasourceId);
}
