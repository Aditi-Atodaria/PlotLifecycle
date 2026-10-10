package com.realestate.api;

import com.realestate.api.ApiRequests.ConvertRequest;
import com.realestate.api.ApiRequests.LeadRequest;
import com.realestate.api.ApiRequests.PaymentRequest;
import com.realestate.api.ApiRequests.ProjectRequest;
import com.realestate.api.ApiRequests.SiteRequest;
import com.realestate.api.ApiRequests.TransitionRequest;
import com.realestate.dto.AgentLeaderboardEntry;
import com.realestate.model.AuditLog;
import com.realestate.model.Buyer;
import com.realestate.model.Commission;
import com.realestate.model.InstallmentSchedule;
import com.realestate.model.Lead;
import com.realestate.model.Payment;
import com.realestate.model.Project;
import com.realestate.model.Site;
import com.realestate.model.SiteStatus;
import com.realestate.repository.BuyerRepository;
import com.realestate.repository.ProjectRepository;
import com.realestate.repository.SiteRepository;
import com.realestate.service.AuditService;
import com.realestate.service.CommissionService;
import com.realestate.service.InstallmentScheduleService;
import com.realestate.service.LeadService;
import com.realestate.service.PaymentService;
import com.realestate.service.SiteLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API under /api/v1. It returns JSON and is meant for other programs
 * (HTTP Basic auth, e.g. curl -u admin:admin123). It reuses the same services
 * as the web pages, so every business rule and audit entry behaves identically.
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final ProjectRepository projectRepository;
    private final SiteRepository siteRepository;
    private final BuyerRepository buyerRepository;
    private final SiteLifecycleService lifecycleService;
    private final LeadService leadService;
    private final PaymentService paymentService;
    private final InstallmentScheduleService installmentScheduleService;
    private final CommissionService commissionService;
    private final AuditService auditService;

    public ApiController(ProjectRepository projectRepository,
                         SiteRepository siteRepository,
                         BuyerRepository buyerRepository,
                         SiteLifecycleService lifecycleService,
                         LeadService leadService,
                         PaymentService paymentService,
                         InstallmentScheduleService installmentScheduleService,
                         CommissionService commissionService,
                         AuditService auditService) {
        this.projectRepository = projectRepository;
        this.siteRepository = siteRepository;
        this.buyerRepository = buyerRepository;
        this.lifecycleService = lifecycleService;
        this.leadService = leadService;
        this.paymentService = paymentService;
        this.installmentScheduleService = installmentScheduleService;
        this.commissionService = commissionService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Projects
    // ------------------------------------------------------------------

    @GetMapping("/projects")
    public List<Project> projects() {
        return projectRepository.findAll();
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectRequest body) {
        Project project = new Project();
        project.setName(body.name().trim());
        project.setLocation(body.location());
        Project saved = projectRepository.save(project);
        auditService.log("PROJECT_CREATED", "Project", saved.getId(),
                "Created project: " + saved.getName() + " in " + saved.getLocation());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ------------------------------------------------------------------
    // Sites (plots)
    // ------------------------------------------------------------------

    @GetMapping("/sites")
    public List<Site> sites(@RequestParam(required = false) SiteStatus status) {
        return status == null ? siteRepository.findAll() : siteRepository.findByStatus(status);
    }

    @PostMapping("/sites")
    public ResponseEntity<Site> createSite(@Valid @RequestBody SiteRequest body) {
        Project project = projectRepository.findById(body.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + body.projectId()));
        Site site = new Site();
        site.setProject(project);
        site.setBlock(body.block());
        site.setAreaSqft(body.areaSqft());
        site.setFacing(body.facing());
        site.setPrice(body.price());
        site.setStatus(SiteStatus.AVAILABLE);
        Site saved = siteRepository.save(site);
        auditService.log("SITE_CREATED", "Site", saved.getId(),
                "Created site #" + saved.getId() + " in project " + project.getName()
                        + " (Block " + body.block() + ", ₹" + body.price() + ")");
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/sites/{id}/transition")
    public Site transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest body) {
        return lifecycleService.transition(id, body.target());
    }

    @DeleteMapping("/sites/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));
        if (site.getStatus() != SiteStatus.AVAILABLE && site.getStatus() != SiteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot delete site #" + id + " because it is currently " + site.getStatus());
        }
        siteRepository.delete(site);
        auditService.log("SITE_DELETED", "Site", id, "Deleted site #" + id + " (was " + site.getStatus() + ")");
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Leads and buyers
    // ------------------------------------------------------------------

    @GetMapping("/leads")
    public List<Lead> leads() {
        return leadService.getAllLeads();
    }

    @PostMapping("/leads")
    public ResponseEntity<Lead> createLead(@Valid @RequestBody LeadRequest body) {
        Lead lead = leadService.createLead(body.name(), body.phone(), body.email(), body.interestedProject(),
                body.source(), body.notes(), body.assignedAgent());
        return ResponseEntity.status(HttpStatus.CREATED).body(lead);
    }

    @PostMapping("/leads/{id}/convert")
    public ResponseEntity<Buyer> convertLead(@PathVariable Long id, @Valid @RequestBody ConvertRequest body) {
        Buyer buyer = leadService.convertLeadToBuyer(id, body.siteId(), body.idProofType(),
                body.idProofNumber(), body.paymentPlan());
        return ResponseEntity.status(HttpStatus.CREATED).body(buyer);
    }

    @GetMapping("/buyers")
    public List<Buyer> buyers() {
        return buyerRepository.findAll();
    }

    // ------------------------------------------------------------------
    // Payments and installments
    // ------------------------------------------------------------------

    @GetMapping("/payments")
    public List<Payment> payments() {
        return paymentService.allPayments();
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> recordPayment(@Valid @RequestBody PaymentRequest body) {
        Payment payment = paymentService.recordPayment(body.buyerId(), body.amount(), body.paymentType());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/installments")
    public List<InstallmentSchedule> installments(@RequestParam(required = false) Long buyerId) {
        return buyerId == null
                ? installmentScheduleService.getAllSchedules()
                : installmentScheduleService.getSchedulesForBuyer(buyerId);
    }

    @PostMapping("/installments/check-overdue")
    public Map<String, Object> checkOverdue() {
        int flagged = installmentScheduleService.flagOverdueInstallments();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flagged", flagged);
        return result;
    }

    // ------------------------------------------------------------------
    // Commissions and audit trail
    // ------------------------------------------------------------------

    @GetMapping("/commissions")
    public List<Commission> commissions() {
        return commissionService.getAllCommissions();
    }

    @GetMapping("/commissions/leaderboard")
    public List<AgentLeaderboardEntry> leaderboard() {
        return commissionService.getLeaderboard();
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs() {
        return new ArrayList<>(auditService.getAllLogs());
    }
}
