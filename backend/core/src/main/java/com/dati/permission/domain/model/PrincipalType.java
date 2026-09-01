package com.dati.permission.domain.model;

import com.dati.auth.domain.service.UserGroupService;

public enum PrincipalType {
    USER, GROUP;  // GROUP 预留（团队），V1 仅支持 USER 与全公开主体

    /** 全公开主体：GROUP 类型的特殊成员，拥有资源的只读访问权（VIEW）。组 id 唯一来源在身份侧。 */
    public static final String ALL_USERS = UserGroupService.ALL_USERS;
}
