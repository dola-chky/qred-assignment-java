package com.dola.qredpaymentbackend.payment.validation;

import com.dola.qredpaymentbackend.payment.domain.PaymentType;
import com.dola.qredpaymentbackend.payment.dto.CreatePaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.util.StringUtils;

public final class PaymentRequestValidator {

  private PaymentRequestValidator() {
  }

  public static boolean hasRequest(CreatePaymentRequest request) {
    return request != null;
  }

  public static boolean hasText(String value) {
    return StringUtils.hasText(normalize(value));
  }

  public static boolean validateSearchCriteria(String clientNumber) {
    return hasText(clientNumber);
  }

  public static boolean hasPaymentDate(LocalDate value) {
    return value != null;
  }

  public static boolean hasPositiveAmount(BigDecimal amount) {
    return amount != null && amount.signum() > 0;
  }

  public static boolean hasSupportedPaymentType(String value) {
    try {
      PaymentType.fromApiValue(value);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public static String normalize(String value) {
    return value == null ? null : value.trim();
  }
}

