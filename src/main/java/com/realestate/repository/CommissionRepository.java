package com.realestate.repository;

import com.realestate.model.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findAllByOrderByCreatedAtDesc();
    List<Commission> findByAgentName(String agentName);
    boolean existsBySiteId(Long siteId);
}
