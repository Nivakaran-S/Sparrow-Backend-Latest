package com.sparrow.payment_service.service;

import com.sparrow.payment_service.model.Payment;
import com.sparrow.payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private MockPaymentGateway mockPaymentGateway;

    public List<Payment> getAllPayments() {
        return repository.findAll();
    }

    public List<Payment> getPaymentsByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public Optional<Payment> getPaymentById(String id) {
        return repository.findById(id);
    }

    public Payment createPayment(Payment payment) {
        // Generate QR code if payment method is QR
        if ("QR_CODE".equalsIgnoreCase(payment.getPaymentMethod())) {
            String qrCodeUrl = qrCodeService.generateQRCode(payment.getId(), payment.getAmount());
            payment.setQrCodeUrl(qrCodeUrl);
        }

        return repository.save(payment);
    }

    public Payment processPayment(String paymentId) {
        return repository.findById(paymentId).map(payment -> {
            // Process payment through mock gateway
            boolean success = mockPaymentGateway.processPayment(
                    payment.getId(),
                    payment.getAmount(),
                    payment.getPaymentMethod()
            );

            if (success) {
                payment.setPaymentStatus("COMPLETED");
                payment.setTransactionId("TXN_" + System.currentTimeMillis());

                // Generate receipt
                String receiptUrl = receiptService.generateReceipt(payment);
                payment.setReceiptUrl(receiptUrl);
            } else {
                payment.setPaymentStatus("FAILED");
            }

            payment.setUpdatedAt(LocalDateTime.now());
            return repository.save(payment);
        }).orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public Optional<Payment> updatePaymentStatus(String id, String status) {
        return repository.findById(id).map(payment -> {
            payment.setPaymentStatus(status);
            payment.setUpdatedAt(LocalDateTime.now());
            return repository.save(payment);
        });
    }

    public byte[] getReceipt(String paymentId) {
        return repository.findById(paymentId)
                .map(payment -> receiptService.getReceiptPdf(payment))
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}