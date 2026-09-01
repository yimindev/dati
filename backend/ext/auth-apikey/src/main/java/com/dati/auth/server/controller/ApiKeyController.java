package com.dati.auth.server.controller;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.ApiKeyService;
import com.dati.auth.server.pojo.ApiKeyCreateRequest;
import com.dati.auth.server.pojo.ApiKeyCreatedResponse;
import com.dati.auth.server.pojo.ApiKeyVO;
import com.dati.base.RequestContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreatedResponse create(@Valid @RequestBody ApiKeyCreateRequest request) {
        User user = RequestContext.getUser();
        ApiKeyService.CreateApiKeyResult result =
            apiKeyService.create(user.getId(), request.name(), request.expiresInDays());
        return new ApiKeyCreatedResponse(
            result.key().id(), result.key().name(), result.plaintext(),
            result.key().keyMask(), result.key().expiresAt());
    }

    @GetMapping
    public List<ApiKeyVO> list() {
        String userId = RequestContext.getUser().getId();
        return apiKeyService.list(userId).stream()
            .map(k -> new ApiKeyVO(k.id(), k.name(), k.keyMask(),
                k.createdAt(), k.expiresAt(), k.lastUsedAt()))
            .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        apiKeyService.delete(id, RequestContext.getUser().getId());
    }
}
