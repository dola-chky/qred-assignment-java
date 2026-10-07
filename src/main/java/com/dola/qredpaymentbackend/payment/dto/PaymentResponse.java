package com.dola.qredpaymentbackend.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    String clientNumber,
    String contractNumber,
    LocalDate paymentDate,
    BigDecimal amount,
    String type) {
}

