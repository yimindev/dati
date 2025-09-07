package com.dataconnai.auth.authentication;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class UserService {

    public Map<String, User> getUserMap(Collection<String> userIds) {
        return Map.of();
    }

}
