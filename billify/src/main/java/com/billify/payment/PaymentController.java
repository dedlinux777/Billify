package com.billify.payment;

import com.billify.dto.PaymentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // User sees their own billing history
    @GetMapping("/my")
    public ResponseEntity<List<PaymentDTO>> getMyPayments(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(paymentService.getMyPayments(email));
    }

    // Get one payment by id
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }
}