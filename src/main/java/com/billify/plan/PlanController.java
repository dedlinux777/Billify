package com.billify.plan;

import com.billify.dto.PlanDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // ADMIN only access:
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(201).body(planService.createPlan(request));
    }

    // Anyone authenticated (admins or users)
    @GetMapping
    public ResponseEntity<Page<PlanDTO>> getAllPlans(
            @PageableDefault(size = 5, sort = "price") Pageable pageable) {
        return ResponseEntity.ok(planService.getAllPlans(pageable));
    }
// get individual plan's DTO
    @GetMapping("/{id}")
    public ResponseEntity<PlanDTO> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }
}