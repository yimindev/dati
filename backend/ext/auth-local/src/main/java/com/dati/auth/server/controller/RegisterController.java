package com.dati.auth.server.controller;

import com.dati.auth.domain.service.RegistrationService;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.pojo.IdResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "auth-controller")
public class RegisterController {

    private final RegistrationService registrationService;

    public RegisterController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public IdResponse register(@Valid @RequestBody RegisterRequest request) {
        String userId = registrationService.register(request);
        return new IdResponse(userId);
    }

}
