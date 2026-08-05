package com.dati.auth.domain.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserGroupService {

    /** 全公开组：所有已登录用户隐式属于此组（只读访问）。 */
    public static final String ALL_USERS = "ALL_USERS";

    /**
     * 返回用户所在的全部组 id（含隐式组 ALL_USERS）。
     * V1 无真实用户组；V2 接入团队后在此合并 DB 查询结果，业务代码零改动。
     */
    public Set<String> groupIdsOf(String userId) {
        return Set.of(ALL_USERS);
    }
}
