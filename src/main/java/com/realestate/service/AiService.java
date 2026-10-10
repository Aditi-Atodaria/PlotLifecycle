package com.realestate.service;

import com.realestate.dto.AgentLeaderboardEntry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the dashboard executive briefing with a deterministic, rule-based engine.
 * No external services are called.
 */
@Service
public class AiService {

    private static final String ENGINE = "Built-in rule-based engine";

    private final DashboardService dashboardService;

    public AiService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ==================================================================
    // Dashboard briefing
    // ==================================================================

    public Map<String, Object> dashboardBriefing(Long projectId) {
        Map<String, Object> kpis = dashboardService.kpis(projectId);
        List<AgentLeaderboardEntry> leaderboard = dashboardService.leaderboard(projectId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("briefing", ruleBasedBriefing(kpis, leaderboard));
        result.put("engine", ENGINE);
        return result;
    }

    private String ruleBasedBriefing(Map<String, Object> kpis, List<AgentLeaderboardEntry> leaderboard) {
        int sellThrough = (Integer) kpis.get("sellThroughPercent");
        long overdue = (Long) kpis.get("overdueInstallments");

        String health;
        if (((Integer) kpis.get("totalSites")) == 0) {
            health = "There are no plots in this view yet.";
        } else if (sellThrough >= 60) {
            health = "Strong: " + sellThrough + "% of plots are booked or sold, with " + kpis.get("availableSites") + " still open.";
        } else if (sellThrough >= 30) {
            health = "Steady: " + sellThrough + "% of plots are booked or sold, with " + kpis.get("availableSites") + " still open.";
        } else {
            health = "Early stage: only " + sellThrough + "% of plots are booked or sold; " + kpis.get("availableSites") + " are open.";
        }

        String cash = "Collected " + rupees(kpis.get("collected")) + " against " + rupees(kpis.get("salesValue"))
                + " of sales value (" + kpis.get("collectionPercent") + "%). Outstanding balance is "
                + rupees(kpis.get("outstanding")) + "; scheduled installments add " + rupees(kpis.get("projectedReceivables"))
                + " still to come.";

        String risk;
        if (overdue > 0) {
            risk = overdue + " installment tranche" + (overdue == 1 ? " is" : "s are") + " overdue. Follow up with those buyers first.";
        } else {
            risk = "No overdue installments. Default risk is low right now.";
        }

        String priority;
        if (overdue > 0) {
            priority = "Collect the overdue tranches this week.";
        } else if (sellThrough < 30) {
            priority = "Push lead conversion; it stands at " + kpis.get("leadConversionPercent") + "%.";
        } else {
            priority = "Keep the pipeline moving and convert warm leads on the open plots.";
        }
        String top = "";
        if (!leaderboard.isEmpty()) {
            AgentLeaderboardEntry best = leaderboard.get(0);
            top = " Top performer: " + best.getAgentName() + " (" + best.getDealsClosed()
                    + (best.getDealsClosed() == 1 ? " deal, " : " deals, ") + rupees(best.getTotalCommission()) + " commission).";
        }

        return "Portfolio health: " + health + "\nCash flow: " + cash + "\nRisk: " + risk + "\nPriority: " + priority + top;
    }

    private String rupees(Object value) {
        java.math.BigDecimal n = value instanceof java.math.BigDecimal ? (java.math.BigDecimal) value : java.math.BigDecimal.ZERO;
        return "₹" + String.format(Locale.forLanguageTag("en-IN"), "%,.0f", n);
    }
}
