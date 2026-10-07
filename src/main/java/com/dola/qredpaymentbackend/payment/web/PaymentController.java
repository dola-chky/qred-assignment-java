package com.dola.qredpaymentbackend.payment.web;

import com.dola.qredpaymentbackend.payment.dto.CreatePaymentRequest;
import com.dola.qredpaymentbackend.payment.dto.PaymentResponse;
import com.dola.qredpaymentbackend.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @GetMapping
  public List<PaymentResponse> getPayments(
      @RequestParam String clientNumber,
      @RequestParam(required = false) String contractNumber) {
    return paymentService.getPayments(clientNumber, contractNumber);
  }

  @PostMapping
  public ResponseEntity<PaymentResponse> createPayment(
      @Valid @RequestBody CreatePaymentRequest request) {
    PaymentResponse response = paymentService.createPayment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}

