package com.realestate.controller;

import com.realestate.model.Project;
import com.realestate.repository.ProjectRepository;
import com.realestate.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public ProjectController(ProjectRepository projectRepository, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects", projectRepository.findAll());
        if (!model.containsAttribute("project")) {
            model.addAttribute("project", new Project());
        }
        return "projects";
    }

    @PostMapping
    public String createProject(@Valid @ModelAttribute("project") Project project,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("projects", projectRepository.findAll());
            return "projects";
        }
        Project saved = projectRepository.save(project);
        auditService.log("PROJECT_CREATED", "Project", saved.getId(),
                "Created project: " + saved.getName() + " in " + saved.getLocation());
        return "redirect:/projects";
    }

    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id, Model model) {
        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
            projectRepository.delete(project);
            auditService.log("PROJECT_DELETED", "Project", id, "Deleted project: " + project.getName());
        } catch (Exception ex) {
            model.addAttribute("error", "Cannot delete project: " + ex.getMessage());
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("project", new Project());
            return "projects";
        }
        return "redirect:/projects";
    }
}
