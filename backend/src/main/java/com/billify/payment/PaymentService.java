package com.billify.payment;

import com.billify.dto.PaymentDTO;
import com.billify.exception.PaymentFailedException;
import com.billify.exception.ResourceNotFoundException;
import com.billify.mapper.PaymentMapper;
import com.billify.model.*;
import com.billify.repository.PaymentRepository;
import com.billify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository    userRepository;

    /**
     * Called INSIDE a @Transactional method in SubscriptionService.
     * If this throws, the entire transaction (including subscription save) rolls back.
     */
    public Payment processPayment(Subscription subscription) {

        log.info("Processing payment for subscription id: {}, plan: {}, amount: {}",
                subscription.getId(),
                subscription.getPlan().getName(),
                subscription.getPlan().getPrice());

        // Simulate payment success/failure (90% success rate)
        boolean paymentSuccess = new Random().nextInt(10) != 0;
//        boolean paymentSuccess = false; // test 402 response if payment fail response works.

        PaymentStatus status = paymentSuccess
                ? PaymentStatus.SUCCESS
                : PaymentStatus.FAILED;

        Payment payment = Payment.builder()
                .subscription(subscription)
                .amount(subscription.getPlan().getPrice())
                .status(status)
                .paymentDate(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        if (status == PaymentStatus.FAILED) {
            log.error("Payment FAILED for user: {}, plan: {}",
                    subscription.getUser().getEmail(),
                    subscription.getPlan().getName());
            // This exception triggers @Transactional rollback
            throw new PaymentFailedException(
                    "Payment processing failed. Please try again.");
        }

        log.info("Payment SUCCESS for user: {}, amount: {}",
                subscription.getUser().getEmail(),
                subscription.getPlan().getPrice());

        return saved;
    }

    public List<PaymentDTO> getMyPayments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return paymentRepository.findAllByUser(user)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found with id: " + id));
        return PaymentMapper.toDTO(payment);
    }
}