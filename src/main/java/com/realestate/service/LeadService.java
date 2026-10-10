package com.realestate.service;

import com.realestate.model.*;
import com.realestate.repository.BuyerRepository;
import com.realestate.repository.LeadRepository;
import com.realestate.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final BuyerRepository buyerRepository;
    private final SiteRepository siteRepository;
    private final SiteLifecycleService lifecycleService;
    private final InstallmentScheduleService installmentScheduleService;
    private final AuditService auditService;

    public LeadService(LeadRepository leadRepository,
                       BuyerRepository buyerRepository,
                       SiteRepository siteRepository,
                       SiteLifecycleService lifecycleService,
                       InstallmentScheduleService installmentScheduleService,
                       AuditService auditService) {
        this.leadRepository = leadRepository;
        this.buyerRepository = buyerRepository;
        this.siteRepository = siteRepository;
        this.lifecycleService = lifecycleService;
        this.installmentScheduleService = installmentScheduleService;
        this.auditService = auditService;
    }

    public Lead createLead(String name, String phone, String email, String interestedProject) {
        return createLead(name, phone, email, interestedProject, LeadSource.WEBSITE, null, null);
    }

    public Lead createLead(String name, String phone, String email, String interestedProject,
                           LeadSource source, String notes, String assignedAgent) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setPhone(phone);
        lead.setEmail(email);
        lead.setInterestedProject(interestedProject);
        lead.setSource(source != null ? source : LeadSource.WEBSITE);
        lead.setNotes(notes);
        lead.setAssignedAgent(assignedAgent);
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.save(lead);
        auditService.log("LEAD_CREATED", "Lead", saved.getId(),
                "Created lead: " + saved.getName() + " (Source: " + saved.getSource() + ", Assigned: " +
                        (saved.getAssignedAgent() != null ? saved.getAssignedAgent() : "None") + ")");

        return saved;
    }

    /**
     * Converts a lead into a buyer, attaches them to an available site, and puts the site ON_HOLD.
     * If the payment plan is INSTALLMENT, automatically generates the 6-month installment schedule.
     */
    @Transactional
    public Buyer convertLeadToBuyer(Long leadId, Long siteId, String idProofType,
                                     String idProofNumber, PaymentPlan paymentPlan) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));

        if (site.getStatus() != SiteStatus.AVAILABLE) {
            throw new IllegalStateException("Site " + siteId + " is not AVAILABLE, cannot assign a buyer");
        }

        Buyer buyer = new Buyer();
        buyer.setSourceLead(lead);
        buyer.setName(lead.getName());
        buyer.setIdProofType(idProofType);
        buyer.setIdProofNumber(idProofNumber);
        buyer.setPaymentPlan(paymentPlan);
        buyer.setSite(site);
        buyer = buyerRepository.save(buyer);

        site.setBuyer(buyer);
        lifecycleService.transition(site.getId(), SiteStatus.ON_HOLD);

        lead.setStatus(LeadStatus.CONVERTED);
        leadRepository.save(lead);

        if (paymentPlan == PaymentPlan.INSTALLMENT) {
            installmentScheduleService.generateSchedule(buyer);
        }

        auditService.log("LEAD_CONVERTED", "Buyer", buyer.getId(),
                "Converted lead '" + lead.getName() + "' to buyer for site #" + site.getId() +
                        " on " + paymentPlan + " plan");

        return buyer;
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    public Optional<Lead> getLeadById(Long id) {
        return leadRepository.findById(id);
    }

    public void deleteLead(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + id));
        leadRepository.delete(lead);
        auditService.log("LEAD_DELETED", "Lead", id, "Deleted lead: " + lead.getName());
    }
}
