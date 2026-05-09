package com.dati.auth.repository.dao;

import com.dati.auth.repository.po.UserPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserPO, String> {

    Optional<UserPO> findByName(String name);

    boolean existsByName(String name);

}
