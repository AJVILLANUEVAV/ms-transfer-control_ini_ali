package com.demo.example.mstransfercontrol.service.impl;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.mapper.TransferMapper;
import com.demo.example.mstransfercontrol.entity.Account;
import com.demo.example.mstransfercontrol.repository.AccountRepository;
import com.demo.example.mstransfercontrol.repository.TransferRepository;
import com.demo.example.mstransfercontrol.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class TransferServiceImpl implements TransferService {

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("17000.00");

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @Override
    @Transactional
    public Mono<TransferResponse> validateAndTransfer(TransferRequest request) {
        return Mono.defer(() -> validateAmount(request))
                .then(accountRepository.findByAccountNumber(request.getSourceAccount()))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Cuenta origen no existe")))
                .flatMap(this::validateActiveAccount)
                .then(Mono.defer(() -> validateDailyLimit(request)))
                .flatMap(ignored -> transferRepository.save(transferMapper.toTransferEntity(request)))
                .map(ignored -> transferMapper.toApprovedResponse(request));
    }

    private Mono<Void> validateAmount(TransferRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Monto debe ser mayor a cero"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateActiveAccount(Account account) {
        if (!"ACTIVE".equals(account.getStatus())) {
            return Mono.error(new IllegalStateException("Cuenta origen no esta activa"));
        }
        return Mono.empty();
    }

    private Mono<BigDecimal> validateDailyLimit(TransferRequest request) {
        LocalDate transferDate = LocalDate.now();
        LocalDateTime startDate = transferDate.atStartOfDay();
        LocalDateTime endDate = transferDate.plusDays(1).atStartOfDay();

        return transferRepository
                .totalTransferredToday(request.getSourceAccount(), startDate, endDate)
                .defaultIfEmpty(BigDecimal.ZERO)
                .flatMap(totalToday -> totalToday.add(request.getAmount()).compareTo(DAILY_LIMIT) > 0
                        ? Mono.error(new IllegalArgumentException("Limite diario excedido"))
                        : Mono.just(totalToday));
    }
}

