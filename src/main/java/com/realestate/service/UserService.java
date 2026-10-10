package com.realestate.service;

import com.realestate.dto.RegisterForm;
import com.realestate.model.User;
import com.realestate.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    /** Roles a visitor may pick when registering. ADMIN is only created by the seeder. */
    private static final Set<String> SELF_REGISTRATION_ROLES = Set.of("ROLE_AGENT", "ROLE_ACCOUNTANT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = user.getRole();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    public User registerUser(RegisterForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + form.getUsername());
        }

        String role = form.getRole().toUpperCase().trim();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        // Self-service sign-up must never be able to create an admin (or any unknown role).
        if (!SELF_REGISTRATION_ROLES.contains(role)) {
            throw new IllegalArgumentException("That role cannot be chosen at sign-up");
        }

        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName().trim());
        user.setRole(role);

        User savedUser = userRepository.save(user);
        auditService.log("USER_REGISTERED", "User", savedUser.getId(),
                "Registered new user: " + savedUser.getUsername() + " with role " + savedUser.getRole());

        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAgents() {
        List<User> agents = userRepository.findByRole("ROLE_AGENT");
        if (agents.isEmpty()) {
            agents = userRepository.findByRole("AGENT");
        }
        return agents;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
