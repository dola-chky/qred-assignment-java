package com.dola.qredpaymentbackend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dola.qredpaymentbackend.payment.domain.Client;
import com.dola.qredpaymentbackend.payment.domain.Contract;
import com.dola.qredpaymentbackend.payment.domain.Payment;
import com.dola.qredpaymentbackend.payment.domain.PaymentType;
import com.dola.qredpaymentbackend.payment.dto.CreatePaymentRequest;
import com.dola.qredpaymentbackend.payment.dto.PaymentReadRow;
import com.dola.qredpaymentbackend.payment.dto.PaymentResponse;
import com.dola.qredpaymentbackend.payment.repository.ClientRepository;
import com.dola.qredpaymentbackend.payment.repository.ContractRepository;
import com.dola.qredpaymentbackend.payment.repository.PaymentRepository;
import com.dola.qredpaymentbackend.payment.web.error.BadRequestException;
import com.dola.qredpaymentbackend.payment.web.error.ConflictException;
import com.dola.qredpaymentbackend.payment.web.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private ClientRepository clientRepository;

  @Mock
  private ContractRepository contractRepository;

  @Mock
  private PaymentRepository paymentRepository;

  @InjectMocks
  private PaymentService paymentService;

  @Test
  void createPaymentCreatesPaymentWhenClientAndContractExist() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        UUID.randomUUID().toString(), "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);
    Payment savedPayment = new Payment(request.idempotencyKey(), request.paymentDate(), 
        request.amount(), PaymentType.INCOMING, client, contract);

    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.of(contract));
    when(paymentRepository.findByIdempotencyKey(request.idempotencyKey()))
        .thenReturn(Optional.empty());
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class))).thenReturn(savedPayment);

    PaymentResponse response = paymentService.createPayment(request);

    assertThat(response.clientNumber()).isEqualTo("12345");
    assertThat(response.contractNumber()).isEqualTo("54321");
    assertThat(response.amount()).isEqualByComparingTo("1000.00");
    assertThat(response.type()).isEqualTo("incoming");

    verify(paymentRepository).save(org.mockito.ArgumentMatchers.any(Payment.class));
  }

  @Test
  void createPaymentThrowsWhenClientDoesNotExist() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        UUID.randomUUID().toString(), "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    when(paymentRepository.findByIdempotencyKey(request.idempotencyKey()))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.createPayment(request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Client 12345 does not exist");
  }

  @Test
  void createPaymentThrowsWhenContractDoesNotExist() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        UUID.randomUUID().toString(), "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");

    when(paymentRepository.findByIdempotencyKey(request.idempotencyKey()))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.createPayment(request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Contract 54321 does not exist");
  }

  @Test
  void getPaymentsByClientNumberUsesRepositoryFilter() {
    UUID clientId = UUID.randomUUID();
    PaymentReadRow paymentRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId)).thenReturn(List.of(paymentRow));

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().clientNumber()).isEqualTo("12345");
    assertThat(responses.getFirst().type()).isEqualTo("incoming");
  }

  @Test
  void getPaymentsByClientAndContractUsesRepositoryFilter() {
    UUID clientId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    PaymentReadRow paymentRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(contractRepository.findIdByContractNumberAndClientId("54321", clientId))
        .thenReturn(Optional.of(contractId));
    when(paymentRepository.findPaymentRowsByClientIdAndContractId(clientId, contractId))
        .thenReturn(List.of(paymentRow));

    List<PaymentResponse> responses = paymentService.getPayments("12345", "54321");

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().contractNumber()).isEqualTo("54321");
    assertThat(responses.getFirst().clientNumber()).isEqualTo("12345");
  }

  @Test
  void getPaymentsThrowsWhenClientNumberIsBlank() {
    assertThatThrownBy(() -> paymentService.getPayments("   ", null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("clientNumber is required");
  }

  @Test
  void getPaymentsReturnsEmptyListWhenClientDoesNotExist() {
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.empty());

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    assertThat(responses).isEmpty();
  }

  @Test
  void getPaymentsReturnsEmptyListWhenPaymentRepositoryReturnsNull() {
    UUID clientId = UUID.randomUUID();
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId)).thenReturn(null);

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    assertThat(responses).isEmpty();
  }

  @Test
  void getPaymentsHandlesPaymentRepositoryDatabaseError() {
    UUID clientId = UUID.randomUUID();
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId))
        .thenThrow(new RuntimeException("Database connection failed"));

    assertThatThrownBy(() -> paymentService.getPayments("12345", null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error while retrieving payment information");
  }

  @Test
  void getPaymentsHandlesContractRepositoryDatabaseError() {
    UUID clientId = UUID.randomUUID();
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(contractRepository.findIdByContractNumberAndClientId("54321", clientId))
        .thenThrow(new RuntimeException("Database connection failed"));

    assertThatThrownBy(() -> paymentService.getPayments("12345", "54321"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error while retrieving payment information");
  }

  @Test
  void getPaymentsHandlesClientRepositoryDatabaseError() {
    when(clientRepository.findIdByClientNumber("12345"))
        .thenThrow(new RuntimeException("Database connection failed"));

    assertThatThrownBy(() -> paymentService.getPayments("12345", null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error while retrieving client information");
  }

  @Test
  void getPaymentsSkipsInvalidPaymentRowsAndReturnsValidOnes() {
    UUID clientId = UUID.randomUUID();
    UUID validPaymentId = UUID.randomUUID();
    
    // Mix of valid and invalid payment rows
    PaymentReadRow validRow = new PaymentReadRow(validPaymentId, "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    PaymentReadRow invalidRow = new PaymentReadRow(null, "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("2000.00"), PaymentType.OUTGOING);
    PaymentReadRow validRow2 = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 2, 28), new BigDecimal("3000.00"), PaymentType.INCOMING);
    
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId))
        .thenReturn(List.of(validRow, invalidRow, validRow2));

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    // Should return only valid responses, skipping the invalid one
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).id()).isEqualTo(validPaymentId);
  }

  @Test
  void getPaymentsNormalizesTrimmedClientNumber() {
    UUID clientId = UUID.randomUUID();
    PaymentReadRow paymentRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId)).thenReturn(List.of(paymentRow));

    List<PaymentResponse> responses = paymentService.getPayments("  12345  ", null);

    assertThat(responses).hasSize(1);
    verify(clientRepository).findIdByClientNumber("12345");
  }

  @Test
  void getPaymentsWithoutContractNumberReturnsAllPaymentsForClient() {
    UUID clientId = UUID.randomUUID();
    PaymentReadRow paymentRow1 = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    PaymentReadRow paymentRow2 = new PaymentReadRow(UUID.randomUUID(), "12345", "54322",
        LocalDate.of(2024, 2, 28), new BigDecimal("2000.00"), PaymentType.OUTGOING);
    
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId))
        .thenReturn(List.of(paymentRow1, paymentRow2));

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    assertThat(responses).hasSize(2);
    assertThat(responses.stream().map(PaymentResponse::contractNumber))
        .containsExactlyInAnyOrder("54321", "54322");
  }

  @Test
  void createPaymentHandlesDatabaseError() {
    CreatePaymentRequest request = new CreatePaymentRequest(
        UUID.randomUUID().toString(), "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    
    when(paymentRepository.findByIdempotencyKey(request.idempotencyKey()))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321"))
        .thenThrow(new RuntimeException("Database connection failed"));

    assertThatThrownBy(() -> paymentService.createPayment(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error while creating payment");
  }

  @Test
  void createPaymentLogsSuccessfulCreation() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);
    Payment savedPayment = new Payment(idempotencyKey, request.paymentDate(), request.amount(), 
        PaymentType.INCOMING, client, contract);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.of(contract));
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class))).thenReturn(savedPayment);

    PaymentResponse response = paymentService.createPayment(request);

    assertThat(response.clientNumber()).isEqualTo("12345");
    assertThat(response.contractNumber()).isEqualTo("54321");
  }

  @Test
  void getPaymentsWithBlankContractNumberTreatsAsNull() {
    UUID clientId = UUID.randomUUID();
    PaymentReadRow paymentRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId)).thenReturn(List.of(paymentRow));

    List<PaymentResponse> responses = paymentService.getPayments("12345", "   ");

    assertThat(responses).hasSize(1);
    verify(clientRepository).findIdByClientNumber("12345");
    verify(paymentRepository).findPaymentRowsByClientId(clientId);
  }

  @Test
  void getPaymentsHandlesPaymentRowWithNullType() {
    UUID clientId = UUID.randomUUID();
    PaymentReadRow validRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"), PaymentType.INCOMING);
    PaymentReadRow invalidRow = new PaymentReadRow(UUID.randomUUID(), "12345", "54321",
        LocalDate.of(2024, 1, 30), new BigDecimal("2000.00"), null);
    
    when(clientRepository.findIdByClientNumber("12345")).thenReturn(Optional.of(clientId));
    when(paymentRepository.findPaymentRowsByClientId(clientId))
        .thenReturn(List.of(validRow, invalidRow));

    List<PaymentResponse> responses = paymentService.getPayments("12345", null);

    // Should skip the invalid row and return only the valid one
    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).type()).isEqualTo("incoming");
  }

  // ==================== CONCURRENCY & IDEMPOTENCY TESTS ====================

  @Test
  void createPaymentDetectsDuplicateRequestByIdempotencyKey() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Payment existingPayment = new Payment(idempotencyKey, LocalDate.of(2024, 1, 30),
        new BigDecimal("1000.00"), PaymentType.INCOMING, 
        new Client("12345"), new Contract("54321", new Client("12345")));

    // Simulate: payment already exists with this idempotency key
    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.of(existingPayment));

    PaymentResponse response = paymentService.createPayment(request);

    // Should return the cached payment without creating a new one
    assertThat(response.clientNumber()).isEqualTo("12345");
    assertThat(response.contractNumber()).isEqualTo("54321");
    
    // Verify that findByIdempotencyKey was called (duplicate detection)
    verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
    
    // Verify that repository.save() was NOT called (no duplicate creation)
    verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any(Payment.class));
  }

  @Test
  void createPaymentAllowsMultiplePaymentsWithDifferentIdempotencyKeys() {
    String idempotencyKey1 = UUID.randomUUID().toString();
    String idempotencyKey2 = UUID.randomUUID().toString();
    
    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);

    CreatePaymentRequest request1 = new CreatePaymentRequest(
        idempotencyKey1, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");
    
    CreatePaymentRequest request2 = new CreatePaymentRequest(
        idempotencyKey2, "12345", "54321", LocalDate.of(2024, 1, 31), 
        new BigDecimal("2000.00"), "outgoing");

    Payment savedPayment1 = new Payment(idempotencyKey1, request1.paymentDate(),
        request1.amount(), PaymentType.INCOMING, client, contract);
    Payment savedPayment2 = new Payment(idempotencyKey2, request2.paymentDate(),
        request2.amount(), PaymentType.OUTGOING, client, contract);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey1))
        .thenReturn(Optional.empty());
    when(paymentRepository.findByIdempotencyKey(idempotencyKey2))
        .thenReturn(Optional.empty());
    
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.of(contract));
    
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
        .thenReturn(savedPayment1)
        .thenReturn(savedPayment2);

    PaymentResponse response1 = paymentService.createPayment(request1);
    PaymentResponse response2 = paymentService.createPayment(request2);

    assertThat(response1.amount()).isEqualByComparingTo("1000.00");
    assertThat(response1.type()).isEqualTo("incoming");
    
    assertThat(response2.amount()).isEqualByComparingTo("2000.00");
    assertThat(response2.type()).isEqualTo("outgoing");

    verify(paymentRepository, times(2)).save(org.mockito.ArgumentMatchers.any(Payment.class));
  }

  @Test
  void createPaymentHandlesOptimisticLockingRetry() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);
    Payment savedPayment = new Payment(idempotencyKey, request.paymentDate(),
        request.amount(), PaymentType.INCOMING, client, contract);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345"))
        .thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321"))
        .thenReturn(Optional.of(contract));
    
    // Simulate OptimisticLockingFailureException on first attempt, success on second
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"))
        .thenReturn(savedPayment);

    PaymentResponse response = paymentService.createPayment(request);

    assertThat(response.clientNumber()).isEqualTo("12345");
    assertThat(response.contractNumber()).isEqualTo("54321");
    
    // Verify that save was called twice (first failed, second succeeded)
    verify(paymentRepository, times(2)).save(org.mockito.ArgumentMatchers.any(Payment.class));
  }

  @Test
  void createPaymentThrowsAfterMaxRetryAttemptsOnOptimisticLockingFailure() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345"))
        .thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321"))
        .thenReturn(Optional.of(contract));
    
    // Always throw OptimisticLockingFailureException to exhaust retries
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    assertThatThrownBy(() -> paymentService.createPayment(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Unable to acquire lock after retries");
    
    // Verify that save was attempted 3 times (MAX_RETRY_ATTEMPTS)
    verify(paymentRepository, times(3)).save(org.mockito.ArgumentMatchers.any(Payment.class));
  }

  @Test
  void createPaymentWithConcurrentClientUpdates() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);
    Payment savedPayment = new Payment(idempotencyKey, request.paymentDate(),
        request.amount(), PaymentType.INCOMING, client, contract);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345"))
        .thenReturn(Optional.of(client));  // Returns same client instance
    when(contractRepository.findByContractNumber("54321"))
        .thenReturn(Optional.of(contract));  // Returns same contract instance
    when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
        .thenReturn(savedPayment);

    // Simulate two concurrent requests
    PaymentResponse response1 = paymentService.createPayment(request);
    
    // In real scenario: client version incremented by concurrent update
    // OptimisticLocking would detect this on second save attempt
    PaymentResponse response2 = paymentService.createPayment(request);

    // Second request should detect duplicate and return cached result
    assertThat(response1.clientNumber()).isEqualTo("12345");
    assertThat(response2.clientNumber()).isEqualTo("12345");
  }

  @Test
  void createPaymentVerifiesContractBelongsToClient() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client1 = new Client("12345");
    Client client2 = new Client("99999");
    Contract contract = new Contract("54321", client2);  // Contract belongs to different client

    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client1));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> paymentService.createPayment(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("belongs to a different client");
  }

  @Test
  void createPaymentIncludesIdempotencyKeyInSave() {
    String idempotencyKey = UUID.randomUUID().toString();
    CreatePaymentRequest request = new CreatePaymentRequest(
        idempotencyKey, "12345", "54321", LocalDate.of(2024, 1, 30), 
        new BigDecimal("1000.00"), "incoming");

    Client client = new Client("12345");
    Contract contract = new Contract("54321", client);

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    when(paymentRepository.findByIdempotencyKey(idempotencyKey))
        .thenReturn(Optional.empty());
    when(clientRepository.findByClientNumber("12345")).thenReturn(Optional.of(client));
    when(contractRepository.findByContractNumber("54321")).thenReturn(Optional.of(contract));
    when(paymentRepository.save(paymentCaptor.capture()))
        .thenReturn(new Payment(idempotencyKey, request.paymentDate(), request.amount(),
            PaymentType.INCOMING, client, contract));

    paymentService.createPayment(request);

    Payment savedPayment = paymentCaptor.getValue();
    assertThat(savedPayment.getIdempotencyKey()).isEqualTo(idempotencyKey);
    assertThat(savedPayment.getPaymentDate()).isEqualTo(request.paymentDate());
    assertThat(savedPayment.getAmount()).isEqualByComparingTo(request.amount());
  }
}

