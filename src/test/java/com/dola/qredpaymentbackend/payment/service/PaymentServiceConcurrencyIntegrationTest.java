package com.dola.qredpaymentbackend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dola.qredpaymentbackend.payment.domain.Client;
import com.dola.qredpaymentbackend.payment.domain.Contract;
import com.dola.qredpaymentbackend.payment.dto.CreatePaymentRequest;
import com.dola.qredpaymentbackend.payment.dto.PaymentResponse;
import com.dola.qredpaymentbackend.payment.repository.ClientRepository;
import com.dola.qredpaymentbackend.payment.repository.ContractRepository;
import com.dola.qredpaymentbackend.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentServiceConcurrencyIntegrationTest {

  private static final String CLIENT_NUMBER = "CONCURRENT-CLIENT";
  private static final String CONTRACT_NUMBER = "CONCURRENT-CONTRACT";

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private ClientRepository clientRepository;

  @Autowired
  private ContractRepository contractRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @BeforeEach
  void setUp() {
    paymentRepository.deleteAll();
    contractRepository.deleteAll();
    clientRepository.deleteAll();

    Client client = clientRepository.save(new Client(CLIENT_NUMBER));
    contractRepository.save(new Contract(CONTRACT_NUMBER, client));
  }

  @AfterEach
  void tearDown() {
    paymentRepository.deleteAll();
    contractRepository.deleteAll();
    clientRepository.deleteAll();
  }

  @Test
  void createPaymentIsIdempotentAcrossConcurrentRequests() throws Exception {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey,
        CLIENT_NUMBER,
        CONTRACT_NUMBER,
        LocalDate.of(2026, 10, 7),
        new BigDecimal("1500.00"),
        "incoming");

    int concurrentCalls = 8;
    CountDownLatch readyGate = new CountDownLatch(concurrentCalls);
    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService executorService = Executors.newFixedThreadPool(concurrentCalls);

    try {
      List<Future<PaymentResponse>> futures = new ArrayList<>();
      for (int i = 0; i < concurrentCalls; i++) {
        futures.add(executorService.submit(createPaymentTask(request, readyGate, startGate)));
      }

      assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
      startGate.countDown();

      List<PaymentResponse> responses = new ArrayList<>();
      for (Future<PaymentResponse> future : futures) {
        responses.add(future.get(10, TimeUnit.SECONDS));
      }

      assertThat(responses).hasSize(concurrentCalls);
      assertThat(responses)
          .extracting(PaymentResponse::id)
          .doesNotContainNull();
      assertThat(responses.stream().map(PaymentResponse::id).distinct().toList())
          .hasSize(1);
      assertThat(responses)
          .extracting(PaymentResponse::clientNumber)
          .containsOnly(CLIENT_NUMBER);
      assertThat(responses)
          .extracting(PaymentResponse::contractNumber)
          .containsOnly(CONTRACT_NUMBER);
      assertThat(paymentRepository.count()).isEqualTo(1);
      assertThat(paymentRepository.findByIdempotencyKey(idempotencyKey)).isPresent();
    } finally {
      executorService.shutdownNow();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private Callable<PaymentResponse> createPaymentTask(CreatePaymentRequest request,
      CountDownLatch readyGate, CountDownLatch startGate) {
    return () -> {
      readyGate.countDown();
      if (!startGate.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting to start concurrent payment creation");
      }

      try {
        return paymentService.createPayment(request);
      } catch (Exception exception) {
        throw new ExecutionException(exception);
      }
    };
  }
}


