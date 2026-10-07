package com.dola.qredpaymentbackend.payment.dto;

import com.dola.qredpaymentbackend.payment.domain.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentReadRow(
    UUID id,
    String clientNumber,
    String contractNumber,
    LocalDate paymentDate,
    BigDecimal amount,
    PaymentType type) {
}

