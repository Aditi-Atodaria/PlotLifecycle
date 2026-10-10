package com.realestate.service;

import com.realestate.dto.AgentLeaderboardEntry;
import com.realestate.model.Buyer;
import com.realestate.model.Commission;
import com.realestate.model.InstallmentSchedule;
import com.realestate.model.InstallmentStatus;
import com.realestate.model.Lead;
import com.realestate.model.LeadStatus;
import com.realestate.model.Payment;
import com.realestate.model.PaymentType;
import com.realestate.model.Project;
import com.realestate.model.Site;
import com.realestate.model.SiteStatus;
import com.realestate.repository.BuyerRepository;
import com.realestate.repository.LeadRepository;
import com.realestate.repository.PaymentRepository;
import com.realestate.repository.ProjectRepository;
import com.realestate.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds every number on the dashboard: inventory, money, lead pipeline,
 * installment risk and the agent leaderboard.
 *
 * Almost everything can be filtered to one project: pass a projectId, or
 * pass null to see all projects together. Plain loops are used (no streams)
 * so the logic is easy to read and explain.
 */
@Service
public class DashboardService {

    private final SiteRepository siteRepository;
    private final PaymentRepository paymentRepository;
    private final LeadRepository leadRepository;
    private final BuyerRepository buyerRepository;
    private final ProjectRepository projectRepository;
    private final InstallmentScheduleService installmentScheduleService;
    private final CommissionService commissionService;

    public DashboardService(SiteRepository siteRepository,
                            PaymentRepository paymentRepository,
                            LeadRepository leadRepository,
                            BuyerRepository buyerRepository,
                            ProjectRepository projectRepository,
                            InstallmentScheduleService installmentScheduleService,
                            CommissionService commissionService) {
        this.siteRepository = siteRepository;
        this.paymentRepository = paymentRepository;
        this.leadRepository = leadRepository;
        this.buyerRepository = buyerRepository;
        this.projectRepository = projectRepository;
        this.installmentScheduleService = installmentScheduleService;
        this.commissionService = commissionService;
    }

    // ------------------------------------------------------------------
    // Filtering helpers
    // ------------------------------------------------------------------

    private boolean inProject(Site site, Long projectId) {
        return projectId == null
                || (site != null && site.getProject() != null && projectId.equals(site.getProject().getId()));
    }

    private List<Site> sitesFor(Long projectId) {
        List<Site> result = new ArrayList<>();
        for (Site site : siteRepository.findAll()) {
            if (inProject(site, projectId)) {
                result.add(site);
            }
        }
        return result;
    }

    private List<Payment> paymentsFor(Long projectId) {
        List<Payment> result = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            Buyer buyer = payment.getBuyer();
            if (projectId == null || (buyer != null && inProject(buyer.getSite(), projectId))) {
                result.add(payment);
            }
        }
        return result;
    }

    /** Leads only store the project NAME, so a project filter matches on the name. */
    private List<Lead> leadsFor(Long projectId) {
        String projectName = null;
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            projectName = project == null ? "" : project.getName();
        }
        List<Lead> result = new ArrayList<>();
        for (Lead lead : leadRepository.findAll()) {
            if (projectName == null || projectName.equalsIgnoreCase(lead.getInterestedProject())) {
                result.add(lead);
            }
        }
        return result;
    }

    private List<InstallmentSchedule> schedulesFor(Long projectId) {
        List<InstallmentSchedule> result = new ArrayList<>();
        // getAllSchedules() also flags anything past its due date as OVERDUE first.
        for (InstallmentSchedule schedule : installmentScheduleService.getAllSchedules()) {
            Buyer buyer = schedule.getBuyer();
            if (projectId == null || (buyer != null && inProject(buyer.getSite(), projectId))) {
                result.add(schedule);
            }
        }
        return result;
    }

    private BigDecimal amountOf(Payment payment) {
        return payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
    }

    private BigDecimal priceOf(Site site) {
        return site.getPrice() == null ? BigDecimal.ZERO : site.getPrice();
    }

    private boolean isSold(Site site) {
        return site.getStatus() == SiteStatus.BOOKED || site.getStatus() == SiteStatus.SOLD;
    }

    private int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    /** Money received from a buyer so far (refunds are not counted as income). */
    private BigDecimal paidBy(Buyer buyer) {
        BigDecimal total = BigDecimal.ZERO;
        for (Payment payment : paymentRepository.findByBuyerId(buyer.getId())) {
            if (payment.getPaymentType() != PaymentType.REFUND) {
                total = total.add(amountOf(payment));
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Headline numbers (KPI cards)
    // ------------------------------------------------------------------

    public Map<String, Object> kpis(Long projectId) {
        List<Site> sites = sitesFor(projectId);

        long available = 0;
        long soldOrBooked = 0;
        BigDecimal salesValue = BigDecimal.ZERO;
        BigDecimal unsoldValue = BigDecimal.ZERO;
        for (Site site : sites) {
            if (site.getStatus() == SiteStatus.AVAILABLE) {
                available++;
                unsoldValue = unsoldValue.add(priceOf(site));
            }
            if (isSold(site)) {
                soldOrBooked++;
                salesValue = salesValue.add(priceOf(site));
            }
        }

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (Payment payment : paymentsFor(projectId)) {
            if (payment.getPaymentType() == PaymentType.REFUND) {
                refunded = refunded.add(amountOf(payment));
            } else {
                collected = collected.add(amountOf(payment));
            }
        }

        BigDecimal outstanding = BigDecimal.ZERO;
        for (Map<String, Object> row : outstandingBuyers(projectId, Integer.MAX_VALUE)) {
            outstanding = outstanding.add((BigDecimal) row.get("outstanding"));
        }

        List<Lead> leads = leadsFor(projectId);
        long converted = 0;
        for (Lead lead : leads) {
            if (lead.getStatus() == LeadStatus.CONVERTED) {
                converted++;
            }
        }

        long overdue = 0;
        BigDecimal projected = BigDecimal.ZERO;
        for (InstallmentSchedule schedule : schedulesFor(projectId)) {
            if (schedule.getStatus() == InstallmentStatus.OVERDUE) {
                overdue++;
            }
            if (schedule.getStatus() != InstallmentStatus.PAID) {
                BigDecimal paid = schedule.getPaidAmount() == null ? BigDecimal.ZERO : schedule.getPaidAmount();
                BigDecimal remaining = schedule.getAmount().subtract(paid);
                if (remaining.signum() > 0) {
                    projected = projected.add(remaining);
                }
            }
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalSites", sites.size());
        kpis.put("availableSites", available);
        kpis.put("soldOrBookedSites", soldOrBooked);
        kpis.put("sellThroughPercent", percent(soldOrBooked, sites.size()));
        kpis.put("unsoldInventoryValue", unsoldValue);
        kpis.put("salesValue", salesValue);
        kpis.put("collected", collected);
        kpis.put("refunded", refunded);
        kpis.put("netCollected", collected.subtract(refunded));
        kpis.put("outstanding", outstanding);
        kpis.put("collectionPercent", salesValue.signum() == 0 ? 0
                : collected.multiply(BigDecimal.valueOf(100)).divide(salesValue, 0, RoundingMode.HALF_UP).intValue());
        kpis.put("totalLeads", leads.size());
        kpis.put("convertedLeads", converted);
        kpis.put("leadConversionPercent", percent(converted, leads.size()));
        kpis.put("overdueInstallments", overdue);
        kpis.put("projectedReceivables", projected);
        return kpis;
    }

    // ------------------------------------------------------------------
    // Chart data
    // ------------------------------------------------------------------

    /** Counts how many sites are in each status (doughnut chart). */
    public Map<String, Long> siteStatusSummary(Long projectId) {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (SiteStatus status : SiteStatus.values()) {
            summary.put(status.name(), 0L);
        }
        for (Site site : sitesFor(projectId)) {
            String key = site.getStatus().name();
            summary.put(key, summary.get(key) + 1);
        }
        return summary;
    }

    /** Money in per day for the last N days (refunds are left out). */
    public Map<String, BigDecimal> revenueTrend(Long projectId, int days) {
        if (days < 1) {
            days = 7;
        }
        if (days > 90) {
            days = 90;
        }
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            trend.put(today.minusDays(i).toString(), BigDecimal.ZERO);
        }
        for (Payment payment : paymentsFor(projectId)) {
            if (payment.getPaymentDate() == null || payment.getPaymentType() == PaymentType.REFUND) {
                continue;
            }
            String key = payment.getPaymentDate().toString();
            if (trend.containsKey(key)) {
                trend.put(key, trend.get(key).add(amountOf(payment)));
            }
        }
        return trend;
    }

    /** How many leads sit at each stage of the pipeline. */
    public Map<String, Long> leadFunnel(Long projectId) {
        Map<String, Long> funnel = new LinkedHashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            funnel.put(status.name(), 0L);
        }
        for (Lead lead : leadsFor(projectId)) {
            String key = lead.getStatus().name();
            funnel.put(key, funnel.get(key) + 1);
        }
        return funnel;
    }

    /** Total amount for each payment type (bar chart). */
    public Map<String, BigDecimal> revenueByType(Long projectId) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (PaymentType type : PaymentType.values()) {
            totals.put(type.name(), BigDecimal.ZERO);
        }
        for (Payment payment : paymentsFor(projectId)) {
            if (payment.getPaymentType() != null) {
                String key = payment.getPaymentType().name();
                totals.put(key, totals.get(key).add(amountOf(payment)));
            }
        }
        return totals;
    }

    // ------------------------------------------------------------------
    // Tables
    // ------------------------------------------------------------------

    /** One row per project, so projects can be compared side by side. */
    public List<Map<String, Object>> projectComparison() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Project project : projectRepository.findAll()) {
            Long id = project.getId();
            List<Site> sites = sitesFor(id);
            long available = 0;
            long soldOrBooked = 0;
            BigDecimal salesValue = BigDecimal.ZERO;
            for (Site site : sites) {
                if (site.getStatus() == SiteStatus.AVAILABLE) {
                    available++;
                }
                if (isSold(site)) {
                    soldOrBooked++;
                    salesValue = salesValue.add(priceOf(site));
                }
            }
            BigDecimal collected = BigDecimal.ZERO;
            for (Payment payment : paymentsFor(id)) {
                if (payment.getPaymentType() != PaymentType.REFUND) {
                    collected = collected.add(amountOf(payment));
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("name", project.getName());
            row.put("location", project.getLocation());
            row.put("totalSites", sites.size());
            row.put("availableSites", available);
            row.put("soldOrBookedSites", soldOrBooked);
            row.put("sellThroughPercent", percent(soldOrBooked, sites.size()));
            row.put("salesValue", salesValue);
            row.put("collected", collected);
            rows.add(row);
        }
        return rows;
    }

    /** Buyers who still owe money on their plot, biggest balance first. */
    public List<Map<String, Object>> outstandingBuyers(Long projectId, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Buyer buyer : buyerRepository.findAll()) {
            Site site = buyer.getSite();
            if (site == null || site.getPrice() == null || !inProject(site, projectId)) {
                continue;
            }
            // Cancelled / available plots no longer owe anything.
            if (site.getStatus() == SiteStatus.CANCELLED || site.getStatus() == SiteStatus.AVAILABLE) {
                continue;
            }
            BigDecimal paid = paidBy(buyer);
            BigDecimal balance = site.getPrice().subtract(paid);
            if (balance.signum() <= 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("buyer", buyer.getName());
            row.put("site", (site.getProject() == null ? "" : site.getProject().getName() + " · ")
                    + "Block " + site.getBlock() + " #" + site.getId());
            row.put("status", site.getStatus().name());
            row.put("price", site.getPrice());
            row.put("paid", paid);
            row.put("outstanding", balance);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (BigDecimal) r.get("outstanding")).reversed());
        if (rows.size() > limit) {
            return new ArrayList<>(rows.subList(0, limit));
        }
        return rows;
    }

    /** The newest payments, for the "latest ledger entries" table. */
    public List<Map<String, Object>> recentPayments(Long projectId, int limit) {
        List<Payment> payments = paymentsFor(projectId);
        payments.sort(Comparator
                .comparing(Payment::getPaymentDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Payment::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .reversed());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Payment payment : payments) {
            if (rows.size() >= limit) {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", payment.getPaymentDate() == null ? "" : payment.getPaymentDate().toString());
            row.put("buyer", payment.getBuyer() == null ? "-" : payment.getBuyer().getName());
            row.put("type", payment.getPaymentType() == null ? "-" : payment.getPaymentType().name());
            row.put("amount", amountOf(payment));
            rows.add(row);
        }
        return rows;
    }

    /** Agents ranked by commission earned (2% on each sold plot), filtered by project. */
    public List<AgentLeaderboardEntry> leaderboard(Long projectId) {
        if (projectId == null) {
            return commissionService.getLeaderboard();
        }
        Map<String, BigDecimal[]> totals = new LinkedHashMap<>();   // [commission, sales, deals]
        for (Commission commission : commissionService.getAllCommissions()) {
            if (!inProject(commission.getSite(), projectId)) {
                continue;
            }
            BigDecimal[] t = totals.computeIfAbsent(commission.getAgentName(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            if (commission.getCommissionAmount() != null) {
                t[0] = t[0].add(commission.getCommissionAmount());
            }
            if (commission.getSaleAmount() != null) {
                t[1] = t[1].add(commission.getSaleAmount());
            }
            t[2] = t[2].add(BigDecimal.ONE);
        }
        List<AgentLeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : totals.entrySet()) {
            BigDecimal[] t = e.getValue();
            entries.add(new AgentLeaderboardEntry(e.getKey(), t[0], t[1], t[2].longValue()));
        }
        entries.sort((a, b) -> b.getTotalCommission().compareTo(a.getTotalCommission()));
        return entries;
    }

    /** Every plot with its status, for the colour-coded site map. */
    public List<Map<String, Object>> siteMap(Long projectId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Site site : sitesFor(projectId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", site.getId());
            row.put("block", site.getBlock());
            row.put("status", site.getStatus().name());
            row.put("project", site.getProject() == null ? "" : site.getProject().getName());
            row.put("price", priceOf(site));
            rows.add(row);
        }
        return rows;
    }

    /** Everything the dashboard page needs, in one JSON response. */
    public Map<String, Object> summary(Long projectId, int days) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("kpis", kpis(projectId));
        summary.put("statusSummary", siteStatusSummary(projectId));
        summary.put("revenueTrend", revenueTrend(projectId, days));
        summary.put("leadFunnel", leadFunnel(projectId));
        summary.put("revenueByType", revenueByType(projectId));
        summary.put("projects", projectComparison());
        summary.put("outstandingBuyers", outstandingBuyers(projectId, 5));
        summary.put("recentPayments", recentPayments(projectId, 6));
        summary.put("leaderboard", leaderboard(projectId));
        summary.put("siteMap", siteMap(projectId));
        summary.put("generatedAt", LocalDateTime.now().withNano(0).toString());
        return summary;
    }
}
