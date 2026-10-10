package com.realestate.api;

import com.realestate.model.LeadSource;
import com.realestate.model.PaymentPlan;
import com.realestate.model.PaymentType;
import com.realestate.model.SiteStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * JSON request bodies for the /api/v1 endpoints. Each record is one body shape;
 * the annotations are the same kind of validation rules the form DTOs use.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    public record ProjectRequest(
            @NotBlank(message = "name is required") String name,
            String location) {
    }

    public record SiteRequest(
            @NotNull(message = "projectId is required") Long projectId,
            @NotBlank(message = "block is required") String block,
            @NotNull(message = "areaSqft is required") @DecimalMin(value = "1", message = "areaSqft must be positive") Double areaSqft,
            @NotBlank(message = "facing is required") String facing,
            @NotNull(message = "price is required") @DecimalMin(value = "1", message = "price must be positive") BigDecimal price) {
    }

    public record TransitionRequest(
            @NotNull(message = "target is required") SiteStatus target) {
    }

    public record LeadRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "phone is required")
            @Pattern(regexp = "^[0-9+ -]{7,15}$", message = "phone must be a valid phone number") String phone,
            String email,
            String interestedProject,
            LeadSource source,
            String notes,
            String assignedAgent) {
    }

    public record ConvertRequest(
            @NotNull(message = "siteId is required") Long siteId,
            @NotBlank(message = "idProofType is required") String idProofType,
            @NotBlank(message = "idProofNumber is required") String idProofNumber,
            @NotNull(message = "paymentPlan is required") PaymentPlan paymentPlan) {
    }

    public record PaymentRequest(
            @NotNull(message = "buyerId is required") Long buyerId,
            @NotNull(message = "amount is required") @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
            @NotNull(message = "paymentType is required") PaymentType paymentType) {
    }
}
