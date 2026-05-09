package com.dati.auth.server.controller;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.AuthenticationService;
import com.dati.auth.domain.service.UserService;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.auth.server.pojo.LoginResponse;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.RequestContext;
import com.dati.base.pojo.IdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService,
                          UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public IdResponse register(@Valid @RequestBody RegisterRequest request) {
        String userId = userService.register(request);
        return new IdResponse(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authenticationService.login(request);
        return new LoginResponse(token);
    }

    @GetMapping("/me")
    public User me() {
        return RequestContext.getUser();
    }

}
