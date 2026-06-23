package com.saas.billing.plan;

import com.saas.billing.dto.PlanDTO;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.mapper.PlanMapper;
import com.saas.billing.model.Plan;
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

    public PlanDTO createPlan(PlanRequestDTO request) {
        // create a plan, build the object from the received from admin's request
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationInDays(request.getDurationInDays())
                .invoiceLimit(request.getInvoiceLimit())
                .apiCallLimit(request.getApiCallLimit())
                .build();
        // save the request
        Plan saved = planRepository.save(plan);
        log.info("New plan created: {} at price {}", saved.getName(), saved.getPrice()); // log it

        return PlanMapper.toDTO(saved); // return back the DTO
    }

    public Page<PlanDTO> getAllPlans(Pageable pageable) {
        log.info("Fetching plans - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return planRepository.findAll(pageable)
                .map(PlanMapper::toDTO);
    }
//    planRepository.findAll(pageable) returns a Page<Plan>. Spring Data builds the LIMIT and OFFSET SQL automatically from the Pageable object. You call .map(PlanMapper::toDTO) to convert every element without writing a loop. The response includes total pages, total elements, and current page — all for free.


    public PlanDTO getPlanById(Long id) {
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