package com.realestate.controller;

import com.realestate.dto.ConvertLeadForm;
import com.realestate.dto.LeadForm;
import com.realestate.model.LeadSource;
import com.realestate.model.SiteStatus;
import com.realestate.repository.LeadRepository;
import com.realestate.repository.SiteRepository;
import com.realestate.service.LeadService;
import com.realestate.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/leads")
public class LeadController {

    private final LeadRepository leadRepository;
    private final SiteRepository siteRepository;
    private final LeadService leadService;
    private final UserService userService;

    public LeadController(LeadRepository leadRepository,
                          SiteRepository siteRepository,
                          LeadService leadService,
                          UserService userService) {
        this.leadRepository = leadRepository;
        this.siteRepository = siteRepository;
        this.leadService = leadService;
        this.userService = userService;
    }

    @GetMapping
    public String listLeads(Model model) {
        model.addAttribute("leads", leadRepository.findAll());
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("agents", userService.getAgents());
        if (!model.containsAttribute("leadForm")) {
            model.addAttribute("leadForm", new LeadForm());
        }
        return "leads";
    }

    @PostMapping
    public String createLead(@Valid @ModelAttribute("leadForm") LeadForm form,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("leads", leadRepository.findAll());
            model.addAttribute("sources", LeadSource.values());
            model.addAttribute("agents", userService.getAgents());
            return "leads";
        }
        leadService.createLead(form.getName(), form.getPhone(), form.getEmail(),
                form.getInterestedProject(), form.getSource(), form.getNotes(), form.getAssignedAgent());
        return "redirect:/leads";
    }

    @GetMapping("/{id}/convert")
    public String convertForm(@PathVariable Long id, Model model) {
        model.addAttribute("lead", leadRepository.findById(id).orElseThrow());
        model.addAttribute("availableSites", siteRepository.findByStatus(SiteStatus.AVAILABLE));
        model.addAttribute("convertForm", new ConvertLeadForm());
        return "convert-lead";
    }

    @PostMapping("/{id}/convert")
    public String convert(@PathVariable Long id,
                           @Valid @ModelAttribute("convertForm") ConvertLeadForm form,
                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("lead", leadRepository.findById(id).orElseThrow());
            model.addAttribute("availableSites", siteRepository.findByStatus(SiteStatus.AVAILABLE));
            return "convert-lead";
        }
        try {
            leadService.convertLeadToBuyer(id, form.getSiteId(), form.getIdProofType(),
                    form.getIdProofNumber(), form.getPaymentPlan());
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("lead", leadRepository.findById(id).orElseThrow());
            model.addAttribute("availableSites", siteRepository.findByStatus(SiteStatus.AVAILABLE));
            return "convert-lead";
        }
        return "redirect:/leads";
    }

    @PostMapping("/{id}/delete")
    public String deleteLead(@PathVariable Long id) {
        leadService.deleteLead(id);
        return "redirect:/leads";
    }
}
