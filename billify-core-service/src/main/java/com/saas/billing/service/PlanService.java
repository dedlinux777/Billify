package com.saas.billing.service;

import com.saas.billing.dto.request.PlanRequest;
import com.saas.billing.dto.response.PlanResponse;
import com.saas.billing.entity.Plan;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.mapper.PlanMapper;
import com.saas.billing.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;

    public PlanResponse createPlan(PlanRequest request) {
        Plan plan = PlanMapper.toEntity(request);
        Plan saved = planRepository.save(plan);
        log.info("New plan created: {} at price {}", saved.getName(), saved.getPrice());
        return PlanMapper.toDTO(saved);
    }

    public Page<PlanResponse> getAllPlans(Pageable pageable) {
        log.info("Fetching plans - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return planRepository.findAll(pageable)
                .map(PlanMapper::toDTO);
    }

    public PlanResponse getPlanById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found with id: " + id));
        return PlanMapper.toDTO(plan);
    }

    public void deletePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found with id: " + id));
        planRepository.delete(plan);
        log.info("Plan deleted: {}", plan.getName());
    }
}
