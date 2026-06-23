package com.saas.usage.repository;

import com.saas.usage.model.UsageSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {
    Optional<UsageSummary> findByUserId(Long userId);
}
