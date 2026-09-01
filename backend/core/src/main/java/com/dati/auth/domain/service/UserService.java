package com.dati.auth.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.server.pojo.UserBriefVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, User> getUserMap(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(UserMapper::toUser)
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /** 按用户名/显示名模糊搜索（授权场景选择被授权人），返回最小字段。 */
    public List<UserBriefVO> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return userRepository.findByNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(po -> {
                    UserBriefVO vo = new UserBriefVO();
                    vo.setId(po.getId());
                    vo.setName(po.getName());
                    vo.setDisplayName(po.getDisplayName());
                    return vo;
                })
                .toList();
    }

}
