package com.realestate.repository;

import com.realestate.model.InstallmentSchedule;
import com.realestate.model.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentScheduleRepository extends JpaRepository<InstallmentSchedule, Long> {
    List<InstallmentSchedule> findByBuyerIdOrderByInstallmentNumberAsc(Long buyerId);
    List<InstallmentSchedule> findByStatus(InstallmentStatus status);
    List<InstallmentSchedule> findAllByOrderByDueDateAsc();
    List<InstallmentSchedule> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate date);
    long countByStatus(InstallmentStatus status);
}
