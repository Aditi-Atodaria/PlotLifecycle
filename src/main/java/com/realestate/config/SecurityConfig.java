package com.realestate.config;

import com.realestate.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login + access-control setup.
 *
 * Users live in the database (see UserService/UserRepository) instead of
 * being hard-coded, so new accounts can register with a role. Each area of
 * the app only opens up to the roles that need it:
 * The REST API under /api/v1 follows the same rules and uses HTTP Basic auth.
 *   ADMIN       - everything, including the audit log
 *   AGENT       - projects, sites, leads, commissions
 *   ACCOUNTANT  - sites, payments, installments, commissions
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/login", "/register").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/projects/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/sites/**").hasAnyRole("ADMIN", "AGENT", "ACCOUNTANT")
                .requestMatchers("/leads/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/payments/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/installments/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/commissions/**").hasAnyRole("ADMIN", "AGENT", "ACCOUNTANT")
                .requestMatchers("/audit/**").hasRole("ADMIN")
                // REST API (HTTP Basic): same role rules as the matching web pages
                .requestMatchers("/api/v1/projects/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/api/v1/sites/**").hasAnyRole("ADMIN", "AGENT", "ACCOUNTANT")
                .requestMatchers("/api/v1/leads/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/api/v1/buyers/**").hasAnyRole("ADMIN", "AGENT", "ACCOUNTANT")
                .requestMatchers("/api/v1/payments/**", "/api/v1/installments/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/api/v1/commissions/**").hasAnyRole("ADMIN", "AGENT", "ACCOUNTANT")
                .requestMatchers("/api/v1/audit-logs/**").hasRole("ADMIN")
                .requestMatchers("/dashboard", "/api/**").authenticated()
                .anyRequest().authenticated()
            )
            // External programs call /api/v1 with HTTP Basic and no browser session, so CSRF is off there.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"))
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
