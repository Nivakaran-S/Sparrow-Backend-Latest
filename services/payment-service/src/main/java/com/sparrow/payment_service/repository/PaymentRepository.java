package com.sparrow.payment_service.repository;

import com.sparrow.payment_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);
    List<Payment> findByParcelId(String parcelId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByPaymentStatus(String paymentStatus);
}