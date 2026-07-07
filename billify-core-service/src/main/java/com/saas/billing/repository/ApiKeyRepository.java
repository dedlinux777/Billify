package com.saas.billing.repository;

import com.saas.billing.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyIdentifier(String keyIdentifier);
    List<ApiKey> findByUserId(Long userId);
}
