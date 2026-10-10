package com.realestate.dto;

import java.math.BigDecimal;

public class AgentLeaderboardEntry {

    private String agentName;
    private BigDecimal totalCommission;
    private BigDecimal totalSalesVolume;
    private long dealsClosed;

    public AgentLeaderboardEntry(String agentName, BigDecimal totalCommission, BigDecimal totalSalesVolume, long dealsClosed) {
        this.agentName = agentName;
        this.totalCommission = totalCommission;
        this.totalSalesVolume = totalSalesVolume;
        this.dealsClosed = dealsClosed;
    }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public BigDecimal getTotalCommission() { return totalCommission; }
    public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }

    public BigDecimal getTotalSalesVolume() { return totalSalesVolume; }
    public void setTotalSalesVolume(BigDecimal totalSalesVolume) { this.totalSalesVolume = totalSalesVolume; }

    public long getDealsClosed() { return dealsClosed; }
    public void setDealsClosed(long dealsClosed) { this.dealsClosed = dealsClosed; }
}
