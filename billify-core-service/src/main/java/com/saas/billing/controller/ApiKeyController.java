package com.saas.billing.controller;

import com.saas.billing.dto.response.ApiKeyCreateResponse;
import com.saas.billing.dto.response.ApiKeyResponse;
import com.saas.billing.service.ApiKeyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/apikeys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/generate")
    public ResponseEntity<ApiKeyCreateResponse> generateApiKey(@RequestParam("userId") Long userId) {
        ApiKeyCreateResponse response = apiKeyService.generateApiKey(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApiKeyResponse>> getKeysByUserId(@PathVariable("userId") Long userId) {
        List<ApiKeyResponse> keys = apiKeyService.getKeysByUserId(userId);
        return ResponseEntity.ok(keys);
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<ApiKeyResponse> revokeApiKey(@PathVariable("id") Long id) {
        ApiKeyResponse response = apiKeyService.revokeApiKey(id);
        return ResponseEntity.ok(response);
    }
}
