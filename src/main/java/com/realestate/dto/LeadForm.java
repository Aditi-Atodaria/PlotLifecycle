package com.realestate.dto;

import com.realestate.model.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LeadForm {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+ -]{7,15}$", message = "Enter a valid phone number")
    private String phone;

    @Email(message = "Enter a valid email")
    private String email;

    private String interestedProject;

    private LeadSource source = LeadSource.WEBSITE;

    private String notes;

    private String assignedAgent;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInterestedProject() { return interestedProject; }
    public void setInterestedProject(String interestedProject) { this.interestedProject = interestedProject; }

    public LeadSource getSource() { return source; }
    public void setSource(LeadSource source) { this.source = source; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String assignedAgent) { this.assignedAgent = assignedAgent; }
}
