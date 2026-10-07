package com.dola.qredpaymentbackend.payment.repository;

import com.dola.qredpaymentbackend.payment.domain.Contract;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

  Optional<Contract> findByContractNumber(String contractNumber);

  @Query("""
      select c.id
      from Contract c
      where c.contractNumber = :contractNumber
        and c.client.id = :clientId
      """)
  Optional<UUID> findIdByContractNumberAndClientId(@Param("contractNumber") String contractNumber,
      @Param("clientId") UUID clientId);
}

