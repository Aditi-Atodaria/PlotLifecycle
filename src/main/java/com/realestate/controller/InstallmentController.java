package com.realestate.controller;

import com.realestate.service.InstallmentScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/installments")
public class InstallmentController {

    private final InstallmentScheduleService installmentScheduleService;

    public InstallmentController(InstallmentScheduleService installmentScheduleService) {
        this.installmentScheduleService = installmentScheduleService;
    }

    @GetMapping
    public String listInstallments(Model model) {
        model.addAttribute("schedules", installmentScheduleService.getAllSchedules());
        model.addAttribute("overdueCount", installmentScheduleService.countOverdueInstallments());
        model.addAttribute("projectedRevenue", installmentScheduleService.calculateProjectedRevenue());
        return "installments";
    }

    @PostMapping("/check-overdue")
    public String checkOverdue() {
        installmentScheduleService.flagOverdueInstallments();
        return "redirect:/installments";
    }
}
