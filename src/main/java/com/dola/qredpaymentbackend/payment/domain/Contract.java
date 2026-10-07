package com.dola.qredpaymentbackend.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(
    name = "contracts",
    uniqueConstraints = @UniqueConstraint(name = "uk_contracts_contract_number", columnNames = "contract_number"))
public class Contract {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "contract_number", nullable = false, unique = true, length = 64)
  private String contractNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "client_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_contracts_clients"))
  private Client client;

  @Version
  @Column(name = "version")
  private Long version;

  protected Contract() {
  }

  public Contract(String contractNumber, Client client) {
    this.contractNumber = contractNumber;
    this.client = client;
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

  public String getContractNumber() {
    return contractNumber;
  }

  public Client getClient() {
    return client;
  }

  public Long getVersion() {
    return version;
  }
}

