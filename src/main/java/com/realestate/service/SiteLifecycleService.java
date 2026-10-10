package com.realestate.service;

import com.realestate.model.Site;
import com.realestate.model.SiteStatus;
import com.realestate.repository.SiteRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the site lifecycle state machine:
 * AVAILABLE   -> ON_HOLD, CANCELLED
 * ON_HOLD     -> LOAN_PENDING, BOOKED, AVAILABLE
 * LOAN_PENDING-> BOOKED, AVAILABLE
 * BOOKED      -> SOLD, CANCELLED
 * SOLD        -> terminal
 * CANCELLED   -> AVAILABLE
 */
@Service
public class SiteLifecycleService {

    private static final Map<SiteStatus, Set<SiteStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(SiteStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(SiteStatus.AVAILABLE, EnumSet.of(SiteStatus.ON_HOLD, SiteStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(SiteStatus.ON_HOLD, EnumSet.of(SiteStatus.LOAN_PENDING, SiteStatus.BOOKED, SiteStatus.AVAILABLE));
        ALLOWED_TRANSITIONS.put(SiteStatus.LOAN_PENDING, EnumSet.of(SiteStatus.BOOKED, SiteStatus.AVAILABLE));
        ALLOWED_TRANSITIONS.put(SiteStatus.BOOKED, EnumSet.of(SiteStatus.SOLD, SiteStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(SiteStatus.SOLD, EnumSet.noneOf(SiteStatus.class));
        ALLOWED_TRANSITIONS.put(SiteStatus.CANCELLED, EnumSet.of(SiteStatus.AVAILABLE));
    }

    private final SiteRepository siteRepository;
    private final CommissionService commissionService;
    private final AuditService auditService;

    public SiteLifecycleService(SiteRepository siteRepository,
                                @Lazy CommissionService commissionService,
                                AuditService auditService) {
        this.siteRepository = siteRepository;
        this.commissionService = commissionService;
        this.auditService = auditService;
    }

    public boolean canTransition(SiteStatus from, SiteStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    @Transactional
    public Site transition(Long siteId, SiteStatus target) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));

        if (!canTransition(site.getStatus(), target)) {
            throw new IllegalStateException(
                    "Cannot move site " + siteId + " from " + site.getStatus() + " to " + target);
        }

        SiteStatus oldStatus = site.getStatus();
        site.setStatus(target);
        Site saved = siteRepository.save(site);

        // Auto-compute commission when site is marked SOLD
        if (target == SiteStatus.SOLD) {
            commissionService.computeCommission(saved);
        }

        auditService.log("SITE_TRANSITION", "Site", saved.getId(),
                "Transitioned site #" + saved.getId() + " from " + oldStatus + " to " + target);

        return saved;
    }

    public Set<SiteStatus> allowedNextStates(SiteStatus current) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
    }
}
