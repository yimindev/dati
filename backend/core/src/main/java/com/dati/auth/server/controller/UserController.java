package com.dati.auth.server.controller;

import com.dati.auth.domain.service.UserService;
import com.dati.auth.server.pojo.UserBriefVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户搜索（授权弹窗选择被授权人用）；登录用户可调，返回最小字段。 */
    @GetMapping("/search")
    public List<UserBriefVO> search(@RequestParam(name = "keyword", required = false) String keyword) {
        return userService.searchUsers(keyword == null ? "" : keyword.trim());
    }
}
