package com.saas.usage.repository;

import com.saas.usage.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
}
