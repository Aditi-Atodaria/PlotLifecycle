package com.realestate.config;

import com.realestate.model.*;
import com.realestate.repository.BuyerRepository;
import com.realestate.repository.LeadRepository;
import com.realestate.repository.ProjectRepository;
import com.realestate.repository.SiteRepository;
import com.realestate.repository.UserRepository;
import com.realestate.service.LeadService;
import com.realestate.service.PaymentService;
import com.realestate.service.SiteLifecycleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final SiteRepository siteRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final PaymentService paymentService;
    private final SiteLifecycleService lifecycleService;
    private final String adminPassword;
    private final String agentPassword;
    private final String accountantPassword;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      ProjectRepository projectRepository,
                      SiteRepository siteRepository,
                      LeadRepository leadRepository,
                      LeadService leadService,
                      PaymentService paymentService,
                      SiteLifecycleService lifecycleService,
                      @Value("${seed.admin-password:admin123}") String adminPassword,
                      @Value("${seed.agent-password:agent123}") String agentPassword,
                      @Value("${seed.accountant-password:accountant123}") String accountantPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.siteRepository = siteRepository;
        this.leadRepository = leadRepository;
        this.leadService = leadService;
        this.paymentService = paymentService;
        this.lifecycleService = lifecycleService;
        this.adminPassword = adminPassword;
        this.agentPassword = agentPassword;
        this.accountantPassword = accountantPassword;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProjectsAndSites();
        seedDemoSale();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        if ("admin123".equals(adminPassword) || "agent123".equals(agentPassword)
                || "accountant123".equals(accountantPassword)) {
            log.warn("Seeding users with the DEFAULT demo passwords. Set SEED_ADMIN_PASSWORD, "
                    + "SEED_AGENT_PASSWORD and SEED_ACCOUNTANT_PASSWORD before exposing this app publicly.");
        }

        User admin = new User("admin", passwordEncoder.encode(adminPassword), "System Admin", "ROLE_ADMIN");
        User agent = new User("agent", passwordEncoder.encode(agentPassword), "Alex Agent", "ROLE_AGENT");
        User accountant = new User("accountant", passwordEncoder.encode(accountantPassword), "Carol Accountant", "ROLE_ACCOUNTANT");

        userRepository.saveAll(List.of(admin, agent, accountant));
    }

    private void seedProjectsAndSites() {
        if (projectRepository.count() > 0) {
            return;
        }

        Project greenValley = new Project();
        greenValley.setName("Green Valley Layout");
        greenValley.setLocation("Whitefield, Bengaluru");
        greenValley = projectRepository.save(greenValley);

        Project riverside = new Project();
        riverside.setName("Riverside Enclave");
        riverside.setLocation("Sarjapur Road, Bengaluru");
        riverside = projectRepository.save(riverside);

        String[] blocks = {"A", "B", "C"};
        String[] facings = {"East", "West", "North", "South"};

        for (Project project : new Project[]{greenValley, riverside}) {
            for (int i = 0; i < 8; i++) {
                Site site = new Site();
                site.setProject(project);
                site.setBlock(blocks[i % blocks.length]);
                site.setAreaSqft(1200.0 + (i * 50));
                site.setFacing(facings[i % facings.length]);
                site.setPrice(BigDecimal.valueOf(1_500_000 + (i * 75_000)));
                site.setStatus(SiteStatus.AVAILABLE);
                siteRepository.save(site);
            }
        }
    }

    private void seedDemoSale() {
        if (leadRepository.count() > 0) {
            return;
        }

        List<Site> availableSites = siteRepository.findByStatus(SiteStatus.AVAILABLE);
        if (availableSites.size() < 2) {
            return;
        }

        // Demo Sale 1: Lead -> Buyer -> 6-Month Installment Schedule -> Down Payment -> 1st Installment Paid
        Lead lead1 = leadService.createLead(
                "Vikram Sharma",
                "+91 98765 43210",
                "vikram.sharma@example.com",
                "Green Valley Layout",
                LeadSource.WEBSITE,
                "Looking for a villa plot with a 6-month installment plan.",
                "agent"
        );

        Site site1 = availableSites.get(0);
        Buyer buyer1 = leadService.convertLeadToBuyer(
                lead1.getId(),
                site1.getId(),
                "Aadhaar Card",
                "9876-5432-1098",
                PaymentPlan.INSTALLMENT
        );

        // Record Down Payment of 20% (₹300,000) -> site automatically moves to BOOKED
        paymentService.recordPayment(buyer1.getId(), BigDecimal.valueOf(300_000), PaymentType.DOWN_PAYMENT);

        // Record 1st Installment payment (₹200,000) -> reconciles 1st installment schedule to PAID
        paymentService.recordPayment(buyer1.getId(), BigDecimal.valueOf(200_000), PaymentType.INSTALLMENT);

        // Demo Sale 2: Lead -> Buyer -> Full Payment -> Transition to SOLD -> Auto Commission computed
        Lead lead2 = leadService.createLead(
                "Ananya Sen",
                "+91 98111 22334",
                "ananya.sen@example.com",
                "Riverside Enclave",
                LeadSource.REFERRAL,
                "Immediate purchase for East-facing plot.",
                "agent"
        );

        Site site2 = availableSites.get(1);
        Buyer buyer2 = leadService.convertLeadToBuyer(
                lead2.getId(),
                site2.getId(),
                "Passport",
                "Z8765432",
                PaymentPlan.FULL_PAYMENT
        );

        // Record down payment & full payment
        paymentService.recordPayment(buyer2.getId(), site2.getPrice(), PaymentType.DOWN_PAYMENT);
        lifecycleService.transition(site2.getId(), SiteStatus.SOLD); // Triggers 2% commission auto-calculation for 'agent'

        // A couple more open leads to show off the pipeline
        leadService.createLead(
                "Rohan Verma",
                "+91 99887 76655",
                "rohan.verma@example.com",
                "Green Valley Layout",
                LeadSource.SOCIAL_MEDIA,
                "Exploring corner plots with good road connectivity.",
                "agent"
        );

        leadService.createLead(
                "Priya Nair",
                "+91 91234 56789",
                "priya.nair@example.com",
                "Riverside Enclave",
                LeadSource.WALK_IN,
                "Visited site during weekend open house; comparing pricing.",
                "agent"
        );
    }
}
