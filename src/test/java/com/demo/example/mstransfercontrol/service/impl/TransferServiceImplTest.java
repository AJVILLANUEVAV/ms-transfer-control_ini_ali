package com.demo.example.mstransfercontrol.service.impl;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.entity.Account;
import com.demo.example.mstransfercontrol.entity.Transfer;
import com.demo.example.mstransfercontrol.mapper.TransferMapper;
import com.demo.example.mstransfercontrol.mapper.impl.TransferMapperImpl;
import com.demo.example.mstransfercontrol.repository.AccountRepository;
import com.demo.example.mstransfercontrol.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final String SOURCE_ACCOUNT = "001-100001";
    private static final String TARGET_ACCOUNT = "001-200001";
    private static final String CUSTOMER_ID = "100";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Spy
    private TransferMapper transferMapper = new TransferMapperImpl();

    @InjectMocks
    private TransferServiceImpl transferService;

    // RN-01: una cuenta origen inexistente debe producir un error reactivo específico.
    @Test
    void shouldFailWhenSourceAccountDoesNotExist() {
        // Arrange
        TransferRequest request = request("100.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT)).thenReturn(Mono.empty());

        // Act
        Mono<TransferResponse> result = transferService.validateAndTransfer(request);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof NoSuchElementException
                        && "Cuenta origen no existe".equals(error.getMessage()))
                .verify();
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verifyNoInteractions(transferRepository);
    }

    // RN-02: cualquier estado distinto de ACTIVE impide realizar la transferencia.
    @ParameterizedTest
    @CsvSource({"BLOCKED", "INACTIVE"})
    void shouldRejectSourceAccountWhenItIsNotActive(String status) {
        // Arrange
        TransferRequest request = request("100.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Mono.just(account(status)));

        // Act
        Mono<TransferResponse> result = transferService.validateAndTransfer(request);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && "Cuenta origen no esta activa".equals(error.getMessage()))
                .verify();
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verifyNoInteractions(transferRepository);
    }

    // RN-03 y RN-06: importes nulos, cero o negativos no deben llegar a los repositorios de transferencias.
    @ParameterizedTest
    @CsvSource(value = {"0.00", "-0.01", "NULL"}, nullValues = "NULL")
    void shouldRejectAmountThatIsNotGreaterThanZero(String amount) {
        // Arrange
        TransferRequest request = request(amount);
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Mono.just(account("ACTIVE")));

        // Act
        Mono<TransferResponse> result = transferService.validateAndTransfer(request);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && "Monto debe ser mayor a cero".equals(error.getMessage()))
                .verify();
        verifyNoInteractions(transferRepository);
    }

    // RN-04 y RN-06: superar el límite diario debe fallar y no guardar una nueva transferencia.
    @Test
    void shouldRejectTransferThatExceedsDailyLimitWithoutSaving() {
        // Arrange
        TransferRequest request = request("1000.01");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Mono.just(account("ACTIVE")));
        when(transferRepository.totalTransferredToday(
                eq(SOURCE_ACCOUNT), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Mono.just(new BigDecimal("16000.00")));

        // Act
        Mono<TransferResponse> result = transferService.validateAndTransfer(request);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && "Limite diario excedido".equals(error.getMessage()))
                .verify();
        verify(transferRepository).totalTransferredToday(
                eq(SOURCE_ACCOUNT), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    // RN-04 y RN-05: un total diario igual al límite se admite y la transferencia queda registrada.
    @Test
    void shouldSaveTransferAndReturnApprovedWhenAllRulesPass() {
        // Arrange
        TransferRequest request = request("5000.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Mono.just(account("ACTIVE")));
        when(transferRepository.totalTransferredToday(
                eq(SOURCE_ACCOUNT), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Mono.just(new BigDecimal("12000.00")));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> Mono.just(invocation.<Transfer>getArgument(0)));

        // Act
        Mono<TransferResponse> result = transferService.validateAndTransfer(request);

        // Assert
        ArgumentCaptor<Transfer> savedTransfer = ArgumentCaptor.forClass(Transfer.class);
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("APPROVED", response.getStatus());
                    assertEquals(SOURCE_ACCOUNT, response.getSourceAccount());
                    assertEquals(TARGET_ACCOUNT, response.getTargetAccount());
                    assertEquals(new BigDecimal("5000.00"), response.getAmount());
                })
                .verifyComplete();
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verify(transferRepository).totalTransferredToday(
                eq(SOURCE_ACCOUNT), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transferRepository).save(savedTransfer.capture());
        assertEquals(SOURCE_ACCOUNT, savedTransfer.getValue().getAccountNumber());
        assertEquals(TARGET_ACCOUNT, savedTransfer.getValue().getAccountDestination());
        assertEquals("successful", savedTransfer.getValue().getStatus());
    }

    private static TransferRequest request(String amount) {
        return TransferRequest.builder()
                .customerId(CUSTOMER_ID)
                .sourceAccount(SOURCE_ACCOUNT)
                .targetAccount(TARGET_ACCOUNT)
                .amount(amount == null ? null : new BigDecimal(amount))
                .build();
    }

    private static Account account(String status) {
        return Account.builder()
                .accountNumber(SOURCE_ACCOUNT)
                .customerId(CUSTOMER_ID)
                .status(status)
                .build();
    }
}
