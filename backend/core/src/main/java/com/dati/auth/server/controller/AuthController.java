package com.dati.auth.server.controller;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.AuthenticationService;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.auth.server.pojo.LoginResponse;
import com.dati.base.RequestContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "auth-controller")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
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
