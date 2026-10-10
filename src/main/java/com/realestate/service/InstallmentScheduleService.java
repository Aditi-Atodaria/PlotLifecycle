package com.realestate.service;

import com.realestate.model.Buyer;
import com.realestate.model.InstallmentSchedule;
import com.realestate.model.InstallmentStatus;
import com.realestate.repository.InstallmentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstallmentScheduleService {

    private final InstallmentScheduleRepository scheduleRepository;
    private final AuditService auditService;

    public InstallmentScheduleService(InstallmentScheduleRepository scheduleRepository,
                                      AuditService auditService) {
        this.scheduleRepository = scheduleRepository;
        this.auditService = auditService;
    }

    /**
     * Generates a 6-month installment schedule off the 80% remaining balance.
     */
    @Transactional
    public List<InstallmentSchedule> generateSchedule(Buyer buyer) {
        if (buyer.getSite() == null || buyer.getSite().getPrice() == null) {
            return List.of();
        }

        BigDecimal totalPrice = buyer.getSite().getPrice();
        BigDecimal balance = totalPrice.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseInstallment = balance.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);

        List<InstallmentSchedule> schedules = new ArrayList<>();
        BigDecimal accumulated = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 6; i++) {
            BigDecimal installmentAmount;
            if (i == 6) {
                // Adjust last installment for any penny rounding discrepancies
                installmentAmount = balance.subtract(accumulated);
            } else {
                installmentAmount = baseInstallment;
                accumulated = accumulated.add(installmentAmount);
            }

            InstallmentSchedule schedule = new InstallmentSchedule();
            schedule.setBuyer(buyer);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(today.plusMonths(i));
            schedule.setAmount(installmentAmount);
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setStatus(InstallmentStatus.PENDING);

            schedules.add(scheduleRepository.save(schedule));
        }

        auditService.log("SCHEDULE_GENERATED", "Buyer", buyer.getId(),
                "Generated 6-month installment schedule totaling ₹" + balance + " for site #" + buyer.getSite().getId());

        return schedules;
    }

    /**
     * Reconciles an incoming payment amount against pending/overdue installments in order.
     */
    @Transactional
    public void reconcilePayment(Buyer buyer, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<InstallmentSchedule> schedules = scheduleRepository.findByBuyerIdOrderByInstallmentNumberAsc(buyer.getId());
        BigDecimal remaining = amount;

        for (InstallmentSchedule schedule : schedules) {
            if (schedule.getStatus() == InstallmentStatus.PAID) {
                continue;
            }

            BigDecimal currentPaid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal due = schedule.getAmount().subtract(currentPaid);

            if (remaining.compareTo(due) >= 0) {
                schedule.setPaidAmount(schedule.getAmount());
                schedule.setStatus(InstallmentStatus.PAID);
                schedule.setPaidDate(LocalDate.now());
                scheduleRepository.save(schedule);
                remaining = remaining.subtract(due);
            } else {
                schedule.setPaidAmount(currentPaid.add(remaining));
                scheduleRepository.save(schedule);
                remaining = BigDecimal.ZERO;
                break;
            }
        }
    }

    /**
     * Flags pending installments past their due date as OVERDUE.
     */
    @Transactional
    public int flagOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<InstallmentSchedule> pastDue = scheduleRepository.findByStatusAndDueDateBefore(InstallmentStatus.PENDING, today);
        for (InstallmentSchedule s : pastDue) {
            s.setStatus(InstallmentStatus.OVERDUE);
            scheduleRepository.save(s);
        }
        return pastDue.size();
    }

    public List<InstallmentSchedule> getAllSchedules() {
        flagOverdueInstallments();
        return scheduleRepository.findAllByOrderByDueDateAsc();
    }

    public List<InstallmentSchedule> getSchedulesForBuyer(Long buyerId) {
        flagOverdueInstallments();
        return scheduleRepository.findByBuyerIdOrderByInstallmentNumberAsc(buyerId);
    }

    public long countOverdueInstallments() {
        flagOverdueInstallments();
        return scheduleRepository.countByStatus(InstallmentStatus.OVERDUE);
    }

    public BigDecimal calculateProjectedRevenue() {
        List<InstallmentSchedule> schedules = scheduleRepository.findAll();
        BigDecimal projected = BigDecimal.ZERO;
        for (InstallmentSchedule s : schedules) {
            if (s.getStatus() != InstallmentStatus.PAID) {
                BigDecimal paid = s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal remaining = s.getAmount().subtract(paid);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    projected = projected.add(remaining);
                }
            }
        }
        return projected;
    }
}
