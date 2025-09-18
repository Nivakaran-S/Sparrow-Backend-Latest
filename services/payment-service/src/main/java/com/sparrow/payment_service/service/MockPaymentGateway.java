package com.sparrow.payment_service.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class MockPaymentGateway {

    private final Random random = new Random();

    public boolean processPayment(String paymentId, Double amount, String paymentMethod) {
        // Simulate payment processing with 90% success rate
        try {
            // Simulate network delay
            Thread.sleep(1000 + random.nextInt(2000));

            // 90% success rate
            return random.nextDouble() < 0.9;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}