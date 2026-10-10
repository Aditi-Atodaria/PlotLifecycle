package com.realestate.controller;

import com.realestate.dto.PaymentForm;
import com.realestate.repository.BuyerRepository;
import com.realestate.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BuyerRepository buyerRepository;

    public PaymentController(PaymentService paymentService, BuyerRepository buyerRepository) {
        this.paymentService = paymentService;
        this.buyerRepository = buyerRepository;
    }

    @GetMapping
    public String listPayments(Model model) {
        model.addAttribute("payments", paymentService.allPayments());
        model.addAttribute("buyers", buyerRepository.findAll());
        if (!model.containsAttribute("paymentForm")) {
            model.addAttribute("paymentForm", new PaymentForm());
        }
        return "payments";
    }

    @PostMapping
    public String recordPayment(@Valid @ModelAttribute("paymentForm") PaymentForm form,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("payments", paymentService.allPayments());
            model.addAttribute("buyers", buyerRepository.findAll());
            return "payments";
        }
        try {
            paymentService.recordPayment(form.getBuyerId(), form.getAmount(), form.getPaymentType());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("payments", paymentService.allPayments());
            model.addAttribute("buyers", buyerRepository.findAll());
            return "payments";
        }
        return "redirect:/payments";
    }
}
