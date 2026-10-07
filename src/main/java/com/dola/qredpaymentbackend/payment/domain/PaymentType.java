package com.dola.qredpaymentbackend.payment.domain;

import java.util.Locale;

public enum PaymentType {
  INCOMING,
  OUTGOING;

  public static PaymentType fromApiValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Payment type is required");
    }
    return PaymentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }

  public String toApiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}

