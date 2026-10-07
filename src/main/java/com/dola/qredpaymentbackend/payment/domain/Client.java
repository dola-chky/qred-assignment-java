package com.dola.qredpaymentbackend.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(
    name = "clients",
    uniqueConstraints = @UniqueConstraint(name = "uk_clients_client_number", columnNames = "client_number"))
public class Client {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "client_number", nullable = false, unique = true, length = 64)
  private String clientNumber;

  @Version
  @Column(name = "version")
  private Long version;

  protected Client() {
  }

  public Client(String clientNumber) {
    this.clientNumber = clientNumber;
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

  public String getClientNumber() {
    return clientNumber;
  }

  public Long getVersion() {
    return version;
  }
}

