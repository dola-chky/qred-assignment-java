package com.dola.qredpaymentbackend.payment.service;

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
import com.dola.qredpaymentbackend.payment.validation.PaymentRequestValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

  private final ClientRepository clientRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;

  public PaymentService(ClientRepository clientRepository, ContractRepository contractRepository,
      PaymentRepository paymentRepository) {
    this.clientRepository = clientRepository;
    this.contractRepository = contractRepository;
    this.paymentRepository = paymentRepository;
  }

  private static final int MAX_RETRY_ATTEMPTS = 3;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public PaymentResponse createPayment(CreatePaymentRequest request) {
    int attemptCount = 0;

    while (attemptCount < MAX_RETRY_ATTEMPTS) {
      try {
        attemptCount++;
        return performPaymentCreation(request);
      } catch (OptimisticLockingFailureException e) {
        logger.debug("Optimistic lock conflict detected on attempt {}/{}", 
            attemptCount, MAX_RETRY_ATTEMPTS);
        
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
          logger.error("Payment creation failed after {} retries due to optimistic locking", 
              MAX_RETRY_ATTEMPTS, e);
          throw new RuntimeException(
              "Payment creation failed: Unable to acquire lock after retries", e);
        }
        // Retry by looping
      }
    }

    throw new RuntimeException("Unexpected: Exited retry loop without result or exception");
  }

  private PaymentResponse performPaymentCreation(CreatePaymentRequest request) {
    logger.debug("Creating payment with idempotencyKey={}, clientNumber={}, contractNumber={}", 
        request.idempotencyKey(), request.clientNumber(), request.contractNumber());
    
    validateCreatePaymentRequest(request);

    // Check for duplicate request using idempotency key
    var existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
    if (existingPayment.isPresent()) {
      logger.info("Duplicate payment request detected for idempotencyKey={}, " +
                  "returning cached result paymentId={}", 
          request.idempotencyKey(), existingPayment.get().getId());
      return toResponse(existingPayment.get());
    }

    String normalizedClientNumber = PaymentRequestValidator.normalize(request.clientNumber());
    String normalizedContractNumber = PaymentRequestValidator.normalize(request.contractNumber());
    PaymentType paymentType = PaymentType.fromApiValue(request.type());

    try {
      Client client = clientRepository.findByClientNumber(normalizedClientNumber)
          .orElseThrow(() -> new NotFoundException(
              "Client " + normalizedClientNumber + " does not exist"));

      Contract contract = contractRepository.findByContractNumber(normalizedContractNumber)
          .orElseThrow(() -> new NotFoundException(
              "Contract " + normalizedContractNumber + " does not exist"));

      if (!Objects.equals(contract.getClient().getClientNumber(), client.getClientNumber())) {
        throw new ConflictException("Contract " + normalizedContractNumber
            + " belongs to a different client");
      }

      Payment savedPayment = paymentRepository.save(
          new Payment(request.idempotencyKey(), request.paymentDate(), request.amount(), 
              paymentType, client, contract));
      
      logger.debug("Payment created successfully: paymentId={}, version={}, idempotencyKey={}, " +
                   "clientNumber={}, contractNumber={}",
          savedPayment.getId(), savedPayment.getVersion(), request.idempotencyKey(),
          normalizedClientNumber, normalizedContractNumber);
      
      return toResponse(savedPayment);
    } catch (NotFoundException | ConflictException e) {
      logger.warn("Payment creation failed: {}", e.getMessage());
      throw e;
    } catch (DataIntegrityViolationException e) {
      var duplicatePayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
      if (duplicatePayment.isPresent()) {
        logger.info("Concurrent duplicate payment request detected for idempotencyKey={}, " +
                "returning existing paymentId={}",
            request.idempotencyKey(), duplicatePayment.get().getId());
        return toResponse(duplicatePayment.get());
      }
      logger.error("Data integrity violation creating payment for idempotencyKey={}, clientNumber={}",
          request.idempotencyKey(), request.clientNumber(), e);
      throw new RuntimeException("Database error while creating payment", e);
    } catch (OptimisticLockingFailureException e) {
      logger.debug("Optimistic locking exception during payment creation, will retry");
      throw e;
    } catch (Exception e) {
      logger.error("Unexpected error creating payment for idempotencyKey={}, clientNumber={}", 
          request.idempotencyKey(), request.clientNumber(), e);
      throw new RuntimeException("Database error while creating payment", e);
    }
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getPayments(String clientNumber, String contractNumber) {
    logger.debug("Fetching payments for clientNumber: {}, contractNumber: {}", 
        clientNumber, contractNumber);
    
    if (!PaymentRequestValidator.validateSearchCriteria(clientNumber)) {
      logger.warn("Invalid search criteria: clientNumber is empty or null");
      throw new BadRequestException("clientNumber is required");
    }

    try {
      String normalizedClientNumber = normalizeSearchParameter(clientNumber);
      String normalizedContractNumber = normalizeSearchParameter(contractNumber);
      
      UUID clientId = resolveClientId(normalizedClientNumber);
      if (clientId == null) {
        logger.info("Client not found for clientNumber: {}", normalizedClientNumber);
        return List.of();
      }

      if (normalizedContractNumber != null && !normalizedContractNumber.isEmpty()) {
        return fetchPaymentsByClientAndContract(clientId, normalizedContractNumber);
      }

      return fetchPaymentsByClient(clientId);
    } catch (BadRequestException e) {
      logger.error("Bad request exception while fetching payments: {}", e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      logger.error("Failed to fetch payments for clientNumber: {}", clientNumber, e);
      throw e;
    }
  }

  private String normalizeSearchParameter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private UUID resolveClientId(String normalizedClientNumber) {
    try {
      return clientRepository.findIdByClientNumber(normalizedClientNumber)
          .orElse(null);
    } catch (RuntimeException e) {
      logger.error("Database error while looking up client: {}", normalizedClientNumber, e);
      throw new RuntimeException("Database error while retrieving client information", e);
    }
  }

  private List<PaymentResponse> fetchPaymentsByClient(UUID clientId) {
    try {
      List<PaymentReadRow> paymentRows = paymentRepository.findPaymentRowsByClientId(clientId);
      return convertToResponses(paymentRows);
    } catch (RuntimeException e) {
      logger.error("Database error while fetching payments for clientId: {}", clientId, e);
      throw new RuntimeException("Database error while retrieving payment information", e);
    }
  }

  private List<PaymentResponse> fetchPaymentsByClientAndContract(UUID clientId, 
      String normalizedContractNumber) {
    try {
      UUID contractId = contractRepository
          .findIdByContractNumberAndClientId(normalizedContractNumber, clientId)
          .orElse(null);

      if (contractId == null) {
        logger.info("Contract not found for contractNumber: {} under clientId: {}", 
            normalizedContractNumber, clientId);
        return List.of();
      }

      List<PaymentReadRow> paymentRows = paymentRepository
          .findPaymentRowsByClientIdAndContractId(clientId, contractId);
      return convertToResponses(paymentRows);
    } catch (RuntimeException e) {
      logger.error("Database error while fetching payments for clientId: {} and contractNumber: {}", 
          clientId, normalizedContractNumber, e);
      throw new RuntimeException("Database error while retrieving payment information", e);
    }
  }

  private List<PaymentResponse> convertToResponses(List<PaymentReadRow> paymentRows) {
    if (paymentRows == null) {
      logger.warn("Payment repository returned null list, returning empty list");
      return List.of();
    }

    List<PaymentResponse> responses = new ArrayList<>();
    for (PaymentReadRow paymentRow : paymentRows) {
      try {
        PaymentResponse response = toResponse(paymentRow);
        responses.add(response);
      } catch (IllegalArgumentException e) {
        logger.error("Error converting paymentRow to response: {}", paymentRow, e);
        // Continue processing other rows, but log the error
      }
    }
    return responses;
  }

  private void validateCreatePaymentRequest(CreatePaymentRequest request) {
    if (!PaymentRequestValidator.hasRequest(request)) {
      logger.warn("Payment request is null");
      throw new BadRequestException("Payment request is required");
    }
    if (!PaymentRequestValidator.hasText(request.clientNumber())) {
      logger.warn("clientNumber is empty or null");
      throw new BadRequestException("clientNumber is required");
    }
    if (!PaymentRequestValidator.hasText(request.contractNumber())) {
      logger.warn("contractNumber is empty or null");
      throw new BadRequestException("contractNumber is required");
    }
    if (!PaymentRequestValidator.hasPaymentDate(request.paymentDate())) {
      logger.warn("paymentDate is null");
      throw new BadRequestException("paymentDate is required");
    }
    if (!PaymentRequestValidator.hasPositiveAmount(request.amount())) {
      logger.warn("Invalid amount: {}", request.amount());
      if (request.amount() == null) {
        throw new BadRequestException("amount is required");
      }
      throw new BadRequestException("amount must be greater than zero");
    }
    if (!PaymentRequestValidator.hasSupportedPaymentType(request.type())) {
      logger.warn("Unsupported payment type: {}", request.type());
      throw new BadRequestException("type must be incoming or outgoing");
    }
  }

  private PaymentResponse toResponse(Payment payment) {
    if (payment == null) {
      logger.error("Payment is null");
      throw new IllegalArgumentException("Payment cannot be null");
    }
    if (payment.getClient() == null) {
      logger.error("Payment has null client: {}", payment.getId());
      throw new IllegalArgumentException("Payment client cannot be null");
    }
    if (payment.getContract() == null) {
      logger.error("Payment has null contract: {}", payment.getId());
      throw new IllegalArgumentException("Payment contract cannot be null");
    }
    
    return new PaymentResponse(
        payment.getId(),
        payment.getClient().getClientNumber(),
        payment.getContract().getContractNumber(),
        payment.getPaymentDate(),
        payment.getAmount(),
        payment.getType().toApiValue());
  }

  private PaymentResponse toResponse(PaymentReadRow paymentRow) {
    if (paymentRow == null) {
      logger.error("PaymentReadRow is null");
      throw new IllegalArgumentException("PaymentReadRow cannot be null");
    }
    if (paymentRow.id() == null) {
      logger.error("PaymentReadRow has null id: {}", paymentRow);
      throw new IllegalArgumentException("PaymentReadRow id cannot be null");
    }
    if (paymentRow.type() == null) {
      logger.error("PaymentReadRow has null type: {}", paymentRow);
      throw new IllegalArgumentException("PaymentReadRow type cannot be null");
    }
    
    return new PaymentResponse(
        paymentRow.id(),
        paymentRow.clientNumber(),
        paymentRow.contractNumber(),
        paymentRow.paymentDate(),
        paymentRow.amount(),
        paymentRow.type().toApiValue());
  }
}


