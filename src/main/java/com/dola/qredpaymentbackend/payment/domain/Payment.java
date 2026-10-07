package com.dola.qredpaymentbackend.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_idempotency_key",
        columnNames = "idempotency_key"))
public class Payment {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 36)
  private String idempotencyKey;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PaymentType type;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_clients"))
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contract_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_contracts"))
  private Contract contract;

  @Version
  @Column(name = "version")
  private Long version;

  protected Payment() {
  }

  public Payment(String idempotencyKey, LocalDate paymentDate, BigDecimal amount, 
      PaymentType type, Client client, Contract contract) {
    this.idempotencyKey = idempotencyKey;
    this.paymentDate = paymentDate;
    this.amount = amount;
    this.type = type;
    this.client = client;
    this.contract = contract;
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  public UUID getId() {
    return id;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentType getType() {
    return type;
  }

  public Client getClient() {
    return client;
  }

  public Contract getContract() {
    return contract;
  }

  public Long getVersion() {
    return version;
  }
}

