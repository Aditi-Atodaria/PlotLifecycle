package com.realestate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private String block;

    private Double areaSqft;

    private String facing;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private SiteStatus status = SiteStatus.AVAILABLE;

    @JsonIgnore
    @OneToOne(mappedBy = "site")
    private Buyer buyer;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public Double getAreaSqft() { return areaSqft; }
    public void setAreaSqft(Double areaSqft) { this.areaSqft = areaSqft; }

    public String getFacing() { return facing; }
    public void setFacing(String facing) { this.facing = facing; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public SiteStatus getStatus() { return status; }
    public void setStatus(SiteStatus status) { this.status = status; }

    public Buyer getBuyer() { return buyer; }
    public void setBuyer(Buyer buyer) { this.buyer = buyer; }
}
