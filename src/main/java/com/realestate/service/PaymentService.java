package com.realestate.service;

import com.realestate.model.*;
import com.realestate.repository.BuyerRepository;
import com.realestate.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles payment recording, applies status transition rules, reconciles installment schedules,
 * and records audit events.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BuyerRepository buyerRepository;
    private final SiteLifecycleService lifecycleService;
    private final InstallmentScheduleService installmentScheduleService;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository,
                          BuyerRepository buyerRepository,
                          SiteLifecycleService lifecycleService,
                          InstallmentScheduleService installmentScheduleService,
                          AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.buyerRepository = buyerRepository;
        this.lifecycleService = lifecycleService;
        this.installmentScheduleService = installmentScheduleService;
        this.auditService = auditService;
    }

    @Transactional
    public Payment recordPayment(Long buyerId, BigDecimal amount, PaymentType type) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found: " + buyerId));

        Payment payment = new Payment();
        payment.setBuyer(buyer);
        payment.setAmount(amount);
        payment.setPaymentType(type);
        payment = paymentRepository.save(payment);

        // Reconcile installment schedule if applicable
        if (type == PaymentType.INSTALLMENT || buyer.getPaymentPlan() == PaymentPlan.INSTALLMENT) {
            installmentScheduleService.reconcilePayment(buyer, amount);
        }

        applyStatusRules(buyer, type);

        Long siteId = buyer.getSite() != null ? buyer.getSite().getId() : null;
        auditService.log("PAYMENT_RECORDED", "Payment", payment.getId(),
                "Recorded " + type + " of ₹" + amount + " for buyer '" + buyer.getName() +
                        "' on site #" + (siteId != null ? siteId : "N/A"));

        return payment;
    }

    /**
     * Decides if this payment should push the site to its next status.
     * Written as plain if/else so it is easy to read and extend later.
     */
    private void applyStatusRules(Buyer buyer, PaymentType type) {
        Site site = buyer.getSite();
        if (site == null) {
            return;
        }

        if (type == PaymentType.DOWN_PAYMENT) {
            if (buyer.getPaymentPlan() == PaymentPlan.LOAN
                    && lifecycleService.canTransition(site.getStatus(), SiteStatus.LOAN_PENDING)) {
                lifecycleService.transition(site.getId(), SiteStatus.LOAN_PENDING);
            } else if (lifecycleService.canTransition(site.getStatus(), SiteStatus.BOOKED)) {
                lifecycleService.transition(site.getId(), SiteStatus.BOOKED);
            }

        } else if (type == PaymentType.LOAN_DISBURSEMENT) {
            if (lifecycleService.canTransition(site.getStatus(), SiteStatus.BOOKED)) {
                lifecycleService.transition(site.getId(), SiteStatus.BOOKED);
            }

        } else if (type == PaymentType.INSTALLMENT) {
            BigDecimal totalPaid = totalPaidByBuyer(buyer);
            if (site.getPrice() != null && totalPaid.compareTo(site.getPrice()) >= 0
                    && lifecycleService.canTransition(site.getStatus(), SiteStatus.SOLD)) {
                lifecycleService.transition(site.getId(), SiteStatus.SOLD);
            }

        } else if (type == PaymentType.REFUND) {
            if (lifecycleService.canTransition(site.getStatus(), SiteStatus.CANCELLED)) {
                lifecycleService.transition(site.getId(), SiteStatus.CANCELLED);
            }
        }
    }

    /** Adds up every payment for this buyer, skipping refunds. */
    public BigDecimal totalPaidByBuyer(Buyer buyer) {
        List<Payment> payments = paymentRepository.findByBuyerId(buyer.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.getPaymentType() != PaymentType.REFUND) {
                total = total.add(payment.getAmount());
            }
        }
        return total;
    }

    public List<Payment> allPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }
}
