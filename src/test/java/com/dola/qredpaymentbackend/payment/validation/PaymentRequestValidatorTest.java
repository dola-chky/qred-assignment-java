package com.dola.qredpaymentbackend.payment.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dola.qredpaymentbackend.payment.dto.CreatePaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  @Test
  void hasRequestReturnsTrueWhenRequestIsPresent() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        UUID.randomUUID().toString(), " 12345 ", " 54321 ", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    assertThat(PaymentRequestValidator.hasRequest(request)).isTrue();
  }

  @Test
  void hasTextReturnsFalseForBlankValue() {
    assertThat(PaymentRequestValidator.hasText("  ")).isFalse();
  }

  @Test
  void validateSearchCriteriaRequiresClientNumber() {
    assertThat(PaymentRequestValidator.validateSearchCriteria(" ")).isFalse();
  }

  @Test
  void validateSearchCriteriaReturnsTrueForValidClientNumber() {
    assertThat(PaymentRequestValidator.validateSearchCriteria("12345")).isTrue();
  }

  @Test
  void hasPositiveAmountReturnsFalseForZero() {
    assertThat(PaymentRequestValidator.hasPositiveAmount(BigDecimal.ZERO)).isFalse();
  }

  @Test
  void hasSupportedPaymentTypeReturnsFalseForInvalidValue() {
    assertThat(PaymentRequestValidator.hasSupportedPaymentType("sideways")).isFalse();
  }

  @Test
  void normalizeTrimsWhitespace() {
    assertThat(PaymentRequestValidator.normalize(" 12345 ")).isEqualTo("12345");
  }
}

