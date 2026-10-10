package com.realestate.controller;

import com.realestate.dto.RegisterForm;
import com.realestate.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthViewController {

    private final UserService userService;
    private final boolean registrationEnabled;
    private final boolean showDemoCredentials;

    public AuthViewController(UserService userService,
                              @Value("${app.registration-enabled:true}") boolean registrationEnabled,
                              @Value("${app.show-demo-credentials:false}") boolean showDemoCredentials) {
        this.userService = userService;
        this.registrationEnabled = registrationEnabled;
        this.showDemoCredentials = showDemoCredentials;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("registrationEnabled", registrationEnabled);
        model.addAttribute("showDemoCredentials", showDemoCredentials);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!registrationEnabled) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result,
                           Model model) {
        if (!registrationEnabled) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerUser(form);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
