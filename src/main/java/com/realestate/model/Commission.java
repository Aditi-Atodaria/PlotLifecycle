package com.realestate.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "commissions")
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agentName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id")
    @JsonIgnoreProperties({"buyer", "project"})
    private Site site;

    private BigDecimal saleAmount;

    private BigDecimal commissionRate = BigDecimal.valueOf(0.02);

    private BigDecimal commissionAmount;

    private LocalDate createdAt = LocalDate.now();

    public Commission() {}

    public Commission(String agentName, Site site, BigDecimal saleAmount, BigDecimal commissionRate, BigDecimal commissionAmount) {
        this.agentName = agentName;
        this.site = site;
        this.saleAmount = saleAmount;
        this.commissionRate = commissionRate;
        this.commissionAmount = commissionAmount;
        this.createdAt = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public BigDecimal getSaleAmount() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount = saleAmount; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}
