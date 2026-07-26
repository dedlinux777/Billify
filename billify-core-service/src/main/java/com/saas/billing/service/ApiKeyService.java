package com.saas.billing.service;

import com.saas.billing.dto.response.ApiKeyCreateResponse;
import com.saas.billing.dto.response.ApiKeyResponse;
import com.saas.billing.entity.ApiKey;
import com.saas.billing.entity.User;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.mapper.ApiKeyMapper;
import com.saas.billing.repository.ApiKeyRepository;
import com.saas.billing.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private static String generateRandomString(int bytes) {
        byte[] randomBytes = new byte[bytes];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    @Transactional
    public ApiKeyCreateResponse generateApiKey(Long userId) {
        log.info("Generating API key for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Public key identifier: blfy_id_XXXXXXXX (approx 12 chars of random string)
        String identifier = "blfy_id_" + generateRandomString(9);
        // Secret part: blfy_sec_XXXXXXXXXXXXXXXX (approx 24 chars of random string)
        String secret = "blfy_sec_" + generateRandomString(18);
        String rawApiKey = identifier + "." + secret;
        
        // Hash the secret
        String keyHash = passwordEncoder.encode(secret);

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .keyIdentifier(identifier)
                .keyHash(keyHash)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Saved API key identifier: {} for user: {}", identifier, userId);

        return ApiKeyCreateResponse.builder()
                .id(saved.getId())
                .userId(saved.getUser().getId())
                .keyIdentifier(saved.getKeyIdentifier())
                .rawApiKey(rawApiKey)
                .active(saved.getActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public ApiKeyResponse generateDefaultKey(Long userId) {
        log.info("Generating default API key for user: {}", userId);
        ApiKeyCreateResponse created = generateApiKey(userId);
        return ApiKeyResponse.builder()
                .id(created.getId())
                .userId(created.getUserId())
                .keyIdentifier(created.getKeyIdentifier())
                .active(created.getActive())
                .createdAt(created.getCreatedAt())
                .build();
    }

    public List<ApiKeyResponse> getKeysByUserId(Long userId) {
        log.info("Fetching API keys for user: {}", userId);
        return apiKeyRepository.findByUserId(userId).stream()
                .map(ApiKeyMapper::toDTO)
                .toList();
    }

    @Transactional
    public ApiKey validateAndRetrieveKey(String rawKey) {
        if (rawKey == null || !rawKey.contains(".")) {
            log.warn("API key validation failed: invalid format");
            throw new IllegalArgumentException("Invalid API key format");
        }

        int dotIdx = rawKey.indexOf('.');
        String identifier = rawKey.substring(0, dotIdx);
        String secret = rawKey.substring(dotIdx + 1);

        ApiKey apiKey = apiKeyRepository.findByKeyIdentifier(identifier)
                .orElseThrow(() -> {
                    log.warn("API key validation failed: identifier {} not found", identifier);
                    return new IllegalArgumentException("Invalid API Key");
                });

        if (!apiKey.getActive()) {
            log.warn("API key validation failed: identifier {} is inactive", identifier);
            throw new IllegalStateException("API Key is inactive");
        }

        if (!passwordEncoder.matches(secret, apiKey.getKeyHash())) {
            log.warn("API key validation failed: secret mismatch for identifier {}", identifier);
            throw new IllegalArgumentException("Invalid API Key");
        }

        // Update lastUsedAt
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        log.info("API key identifier {} successfully validated for user: {}", identifier, apiKey.getUser().getId());
        return apiKey;
    }

    @Transactional
    public ApiKeyResponse revokeApiKey(Long id) {
        log.info("Revoking API key: {}", id);
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key not found with id: " + id));
        apiKey.setActive(false);
        ApiKey updated = apiKeyRepository.save(apiKey);
        return ApiKeyMapper.toDTO(updated);
    }
}
