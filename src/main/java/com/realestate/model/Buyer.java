package com.realestate.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buyers")
public class Buyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "lead_id")
    private Lead sourceLead;

    private String name;
    private String idProofType;
    private String idProofNumber;

    @Enumerated(EnumType.STRING)
    private PaymentPlan paymentPlan;

    @OneToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    private LocalDate createdDate = LocalDate.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lead getSourceLead() { return sourceLead; }
    public void setSourceLead(Lead sourceLead) { this.sourceLead = sourceLead; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdProofType() { return idProofType; }
    public void setIdProofType(String idProofType) { this.idProofType = idProofType; }

    public String getIdProofNumber() { return idProofNumber; }
    public void setIdProofNumber(String idProofNumber) { this.idProofNumber = idProofNumber; }

    public PaymentPlan getPaymentPlan() { return paymentPlan; }
    public void setPaymentPlan(PaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
}
