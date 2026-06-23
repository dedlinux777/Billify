package com.saas.usage.service;

import com.saas.usage.dto.UsageEventRequestDTO;
import com.saas.usage.model.UsageEvent;
import com.saas.usage.repository.UsageEventRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageEventService {

    private final UsageEventRepository usageEventRepository;

    @Transactional
    public void saveEvent(UsageEventRequestDTO request) {
        log.info("Saving usage event for user: {}, resource: {}", request.getUserId(), request.getResourceType());
        
        UsageEvent event = UsageEvent.builder()
                .userId(request.getUserId())
                .resourceType(request.getResourceType())
                .createdAt(LocalDateTime.now())
                .build();

        usageEventRepository.save(event);
        log.info("Usage event saved successfully in database");
    }
}
