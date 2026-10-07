package com.dola.qredpaymentbackend.payment.repository;

import com.dola.qredpaymentbackend.payment.domain.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {

  Optional<Client> findByClientNumber(String clientNumber);

  @Query("select c.id from Client c where c.clientNumber = :clientNumber")
  Optional<UUID> findIdByClientNumber(@Param("clientNumber") String clientNumber);
}

