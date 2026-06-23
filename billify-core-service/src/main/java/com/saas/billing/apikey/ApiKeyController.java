package com.saas.billing.apikey;

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
}
