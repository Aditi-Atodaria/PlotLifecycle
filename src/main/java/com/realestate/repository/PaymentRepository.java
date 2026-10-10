package com.realestate.repository;

import com.realestate.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByPaymentDateDesc();
    List<Payment> findByBuyerId(Long buyerId);
}
