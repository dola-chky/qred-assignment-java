package com.dola.qredpaymentbackend.payment.web.error;

public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}

