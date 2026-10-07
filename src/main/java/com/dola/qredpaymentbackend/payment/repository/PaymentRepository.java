package com.dola.qredpaymentbackend.payment.repository;

import com.dola.qredpaymentbackend.payment.domain.Payment;
import com.dola.qredpaymentbackend.payment.dto.PaymentReadRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  @Query("""
      select new com.dola.qredpaymentbackend.payment.dto.PaymentReadRow(
          p.id,
          c.clientNumber,
          ct.contractNumber,
          p.paymentDate,
          p.amount,
          p.type
      )
      from Payment p
      join p.client c
      join p.contract ct
      where c.id = :clientId
      order by p.paymentDate desc
      """)
  List<PaymentReadRow> findPaymentRowsByClientId(@Param("clientId") UUID clientId);

  @Query("""
      select new com.dola.qredpaymentbackend.payment.dto.PaymentReadRow(
          p.id,
          c.clientNumber,
          ct.contractNumber,
          p.paymentDate,
          p.amount,
          p.type
      )
      from Payment p
      join p.client c
      join p.contract ct
      where c.id = :clientId
        and ct.id = :contractId
      order by p.paymentDate desc
      """)
  List<PaymentReadRow> findPaymentRowsByClientIdAndContractId(@Param("clientId") UUID clientId,
      @Param("contractId") UUID contractId);

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}

