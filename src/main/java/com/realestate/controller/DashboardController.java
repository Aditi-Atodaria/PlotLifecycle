package com.realestate.controller;

import com.realestate.repository.ProjectRepository;
import com.realestate.service.AiService;
import com.realestate.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProjectRepository projectRepository;
    private final AiService aiService;

    public DashboardController(DashboardService dashboardService,
                               ProjectRepository projectRepository,
                               AiService aiService) {
        this.dashboardService = dashboardService;
        this.projectRepository = projectRepository;
        this.aiService = aiService;
    }

    /** The page itself. The numbers are loaded by JavaScript from /api/dashboard/summary. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("projects", projectRepository.findAll());
        return "dashboard";
    }

    /** One call that returns every card, chart and table. projectId is optional. */
    @GetMapping("/api/dashboard/summary")
    @ResponseBody
    public Map<String, Object> summary(@RequestParam(required = false) Long projectId,
                                       @RequestParam(defaultValue = "7") int days) {
        return dashboardService.summary(projectId, days);
    }

    /** Executive briefing from the built-in rule-based engine. */
    @GetMapping("/api/dashboard/ai-summary")
    @ResponseBody
    public Map<String, Object> aiSummary(@RequestParam(required = false) Long projectId) {
        return aiService.dashboardBriefing(projectId);
    }

    // Earlier-week endpoints, kept so nothing that used them breaks.
    @GetMapping("/api/dashboard/status-summary")
    @ResponseBody
    public Map<String, Long> statusSummary(@RequestParam(required = false) Long projectId) {
        return dashboardService.siteStatusSummary(projectId);
    }

    @GetMapping("/api/dashboard/revenue-trend")
    @ResponseBody
    public Map<String, BigDecimal> revenueTrend(@RequestParam(required = false) Long projectId,
                                                @RequestParam(defaultValue = "7") int days) {
        return dashboardService.revenueTrend(projectId, days);
    }
}
