package com.realestate.controller;

import com.realestate.model.Project;
import com.realestate.model.Site;
import com.realestate.model.SiteStatus;
import com.realestate.repository.ProjectRepository;
import com.realestate.repository.SiteRepository;
import com.realestate.service.AuditService;
import com.realestate.service.SiteLifecycleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class SiteController {

    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;
    private final SiteLifecycleService lifecycleService;
    private final AuditService auditService;

    public SiteController(SiteRepository siteRepository,
                          ProjectRepository projectRepository,
                          SiteLifecycleService lifecycleService,
                          AuditService auditService) {
        this.siteRepository = siteRepository;
        this.projectRepository = projectRepository;
        this.lifecycleService = lifecycleService;
        this.auditService = auditService;
    }

    @GetMapping("/sites")
    public String listSites(Model model) {
        model.addAttribute("sites", siteRepository.findAll());
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("allStatuses", SiteStatus.values());
        return "sites";
    }

    @PostMapping("/sites")
    public String createSite(@RequestParam Long projectId,
                             @RequestParam String block,
                             @RequestParam Double areaSqft,
                             @RequestParam String facing,
                             @RequestParam BigDecimal price,
                             Model model) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

            Site site = new Site();
            site.setProject(project);
            site.setBlock(block);
            site.setAreaSqft(areaSqft);
            site.setFacing(facing);
            site.setPrice(price);
            site.setStatus(SiteStatus.AVAILABLE);

            Site saved = siteRepository.save(site);
            auditService.log("SITE_CREATED", "Site", saved.getId(),
                    "Created site #" + saved.getId() + " in project " + project.getName() + " (Block " + block + ", ₹" + price + ")");
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("sites", siteRepository.findAll());
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("allStatuses", SiteStatus.values());
            return "sites";
        }
        return "redirect:/sites";
    }

    @PostMapping("/sites/{id}/transition")
    public String transition(@PathVariable Long id,
                              @RequestParam SiteStatus target,
                              Model model) {
        try {
            lifecycleService.transition(id, target);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("sites", siteRepository.findAll());
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("allStatuses", SiteStatus.values());
            return "sites";
        }
        return "redirect:/sites";
    }

    @PostMapping("/sites/{id}/delete")
    public String deleteSite(@PathVariable Long id, Model model) {
        try {
            Site site = siteRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));

            if (site.getStatus() == SiteStatus.BOOKED || site.getStatus() == SiteStatus.SOLD) {
                throw new IllegalStateException("Cannot delete site #" + id + " because it is currently " + site.getStatus());
            }

            siteRepository.delete(site);
            auditService.log("SITE_DELETED", "Site", id, "Deleted site #" + id + " (was " + site.getStatus() + ")");
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("sites", siteRepository.findAll());
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("allStatuses", SiteStatus.values());
            return "sites";
        }
        return "redirect:/sites";
    }
}
