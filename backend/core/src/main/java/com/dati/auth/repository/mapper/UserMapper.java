package com.dati.auth.repository.mapper;

import com.dati.auth.authentication.User;
import com.dati.auth.repository.po.UserPO;

public class UserMapper {

    public static UserPO toUserPO(User user, String passwordHash) {
        if (user == null) {
            return null;
        }
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setName(user.getName());
        po.setPassword(passwordHash);
        po.setDisplayName(user.getDisplayName());
        return po;
    }

    public static User toUser(UserPO po) {
        if (po == null) {
            return null;
        }
        User user = new User();
        user.setId(po.getId());
        user.setName(po.getName());
        user.setDisplayName(po.getDisplayName());
        return user;
    }

}
