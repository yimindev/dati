package com.dati.auth.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserPO extends BasePO {

    @Column(length = 64, unique = true, nullable = false)
    private String name;

    @Column(length = 256, nullable = false)
    private String password;

    @Column(length = 64)
    private String displayName;

}
