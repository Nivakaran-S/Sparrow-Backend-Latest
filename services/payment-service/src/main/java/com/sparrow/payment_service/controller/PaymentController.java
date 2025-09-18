package com.sparrow.payment_service.controller;

import com.sparrow.payment_service.model.Payment;
import com.sparrow.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment API", description = "API for processing parcel payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all payments")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get payments by user ID")
    public List<Payment> getPaymentsByUserId(@PathVariable String userId) {
        return paymentService.getPaymentsByUserId(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String id) {
        Optional<Payment> payment = paymentService.getPaymentById(id);
        return payment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new payment")
    public Payment createPayment(@Valid @RequestBody Payment payment) {
        return paymentService.createPayment(payment);
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process a payment")
    public ResponseEntity<Payment> processPayment(@PathVariable String id) {
        try {
            Payment processedPayment = paymentService.processPayment(id);
            return ResponseEntity.ok(processedPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Download payment receipt")
    public ResponseEntity<ByteArrayResource> downloadReceipt(@PathVariable String id) {
        try {
            byte[] receiptData = paymentService.getReceipt(id);

            ByteArrayResource resource = new ByteArrayResource(receiptData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=receipt_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(receiptData.length)
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update payment status")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable String id,
            @RequestParam String status) {
        Optional<Payment> updatedPayment = paymentService.updatePaymentStatus(id, status);
        return updatedPayment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}