package com.realestate.controller;

import com.realestate.service.CommissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/commissions")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping
    public String listCommissions(Model model) {
        model.addAttribute("commissions", commissionService.getAllCommissions());
        model.addAttribute("leaderboard", commissionService.getLeaderboard());
        model.addAttribute("totalCommissionsPaid", commissionService.getTotalCommissionsPaid());
        return "commissions";
    }
}
