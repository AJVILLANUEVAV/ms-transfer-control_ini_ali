package com.demo.example.mstransfercontrol.repository;

import com.demo.example.mstransfercontrol.entity.Transfer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransferRepository extends ReactiveCrudRepository<Transfer, Long> {

    @Query("""
       SELECT COALESCE(SUM(amount), 0)
       FROM transfers
       WHERE account_number = :accountNumber
         AND status = 'successful'
         AND transfer_date >= :startDate
         AND transfer_date < :endDate
       """)
    Mono<BigDecimal> totalTransferredToday(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
