package com.realestate.service;

import com.realestate.dto.AgentLeaderboardEntry;
import com.realestate.model.Buyer;
import com.realestate.model.Commission;
import com.realestate.model.Site;
import com.realestate.repository.CommissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommissionService {

    private final CommissionRepository commissionRepository;
    private final AuditService auditService;

    public CommissionService(CommissionRepository commissionRepository, AuditService auditService) {
        this.commissionRepository = commissionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Commission computeCommission(Site site) {
        if (site == null || site.getId() == null) {
            return null;
        }

        if (commissionRepository.existsBySiteId(site.getId())) {
            return null;
        }

        Buyer buyer = site.getBuyer();
        String agentName = "General Agent";
        if (buyer != null && buyer.getSourceLead() != null && buyer.getSourceLead().getAssignedAgent() != null
                && !buyer.getSourceLead().getAssignedAgent().isBlank()) {
            agentName = buyer.getSourceLead().getAssignedAgent().trim();
        }

        BigDecimal saleAmount = site.getPrice() != null ? site.getPrice() : BigDecimal.ZERO;
        BigDecimal rate = BigDecimal.valueOf(0.02); // 2% commission
        BigDecimal commissionAmount = saleAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        Commission commission = new Commission(agentName, site, saleAmount, rate, commissionAmount);
        Commission saved = commissionRepository.save(commission);

        auditService.log("COMMISSION_COMPUTED", "Commission", saved.getId(),
                "Computed ₹" + commissionAmount + " commission (2%) for agent '" + agentName + "' on site #" + site.getId());

        return saved;
    }

    public List<Commission> getAllCommissions() {
        return commissionRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Groups every commission by agent name and totals each agent's sales/commission. */
    public List<AgentLeaderboardEntry> getLeaderboard() {
        Map<String, List<Commission>> byAgent = new LinkedHashMap<>();
        for (Commission commission : commissionRepository.findAll()) {
            byAgent.computeIfAbsent(commission.getAgentName(), name -> new ArrayList<>()).add(commission);
        }

        List<AgentLeaderboardEntry> leaderboard = new ArrayList<>();
        for (Map.Entry<String, List<Commission>> entry : byAgent.entrySet()) {
            String agent = entry.getKey();
            List<Commission> agentComms = entry.getValue();

            BigDecimal totalCommission = BigDecimal.ZERO;
            BigDecimal totalSales = BigDecimal.ZERO;
            for (Commission commission : agentComms) {
                if (commission.getCommissionAmount() != null) {
                    totalCommission = totalCommission.add(commission.getCommissionAmount());
                }
                if (commission.getSaleAmount() != null) {
                    totalSales = totalSales.add(commission.getSaleAmount());
                }
            }

            leaderboard.add(new AgentLeaderboardEntry(agent, totalCommission, totalSales, agentComms.size()));
        }

        leaderboard.sort((a, b) -> b.getTotalCommission().compareTo(a.getTotalCommission()));
        return leaderboard;
    }

    /** Adds up the commission amount across every commission ever recorded. */
    public BigDecimal getTotalCommissionsPaid() {
        BigDecimal total = BigDecimal.ZERO;
        for (Commission commission : commissionRepository.findAll()) {
            if (commission.getCommissionAmount() != null) {
                total = total.add(commission.getCommissionAmount());
            }
        }
        return total;
    }
}
