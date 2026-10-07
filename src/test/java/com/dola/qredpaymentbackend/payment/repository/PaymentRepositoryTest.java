package com.dola.qredpaymentbackend.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dola.qredpaymentbackend.payment.domain.Client;
import com.dola.qredpaymentbackend.payment.domain.Contract;
import com.dola.qredpaymentbackend.payment.domain.Payment;
import com.dola.qredpaymentbackend.payment.domain.PaymentType;
import com.dola.qredpaymentbackend.payment.dto.PaymentReadRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentRepositoryTest {

  @Autowired
  private ClientRepository clientRepository;

  @Autowired
  private ContractRepository contractRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Test
  void findsProjectedPaymentsByClientAndContractIds() {
    Client client = clientRepository.save(new Client("12345"));
    Contract contract = contractRepository.save(new Contract("54321", client));
    paymentRepository.save(new Payment(
        UUID.randomUUID().toString(), LocalDate.of(2024, 1, 30), new BigDecimal("1000.00"),
        PaymentType.INCOMING, client, contract));

    UUID clientId = clientRepository.findIdByClientNumber("12345").orElseThrow();
    UUID contractId = contractRepository.findIdByContractNumberAndClientId("54321", clientId)
        .orElseThrow();

    List<PaymentReadRow> byClient = paymentRepository.findPaymentRowsByClientId(clientId);
    List<PaymentReadRow> byClientAndContract = paymentRepository
        .findPaymentRowsByClientIdAndContractId(clientId, contractId);

    assertThat(byClient).hasSize(1);
    assertThat(byClientAndContract).hasSize(1);
    assertThat(byClient.getFirst().amount()).isEqualByComparingTo("1000.00");
    assertThat(byClientAndContract.getFirst().contractNumber()).isEqualTo("54321");
  }
}



