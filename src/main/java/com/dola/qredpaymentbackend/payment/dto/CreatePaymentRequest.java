package com.dola.qredpaymentbackend.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
    @NotBlank String idempotencyKey,
    @NotBlank String clientNumber,
    @NotBlank String contractNumber,
    @NotNull LocalDate paymentDate,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank @Pattern(regexp = "incoming|outgoing") String type) {
}

