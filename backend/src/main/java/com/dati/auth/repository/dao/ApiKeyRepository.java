package com.dati.auth.repository.dao;

import com.dati.auth.repository.po.ApiKeyPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyPO, String> {

    Optional<ApiKeyPO> findByKeyHash(String keyHash);

    List<ApiKeyPO> findByUserIdOrderByCreatedAtDesc(String userId);

}
