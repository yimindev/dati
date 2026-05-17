package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectDAO extends JpaRepository<SubjectPO, String> {

    @Query("SELECT s FROM SubjectPO s WHERE " +
           "s.id LIKE CONCAT(:keyword, '%') OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SubjectPO> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
