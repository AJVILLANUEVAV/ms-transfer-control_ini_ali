package com.demo.example.mstransfercontrol.controller;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(transferController).build();
    }

    // Verifica que el endpoint transforme una respuesta aprobada del servicio en HTTP 200.
    @Test
    void shouldReturnApprovedResponseWhenTransferSucceeds() {
        // Arrange
        TransferRequest expectedRequest = request();
        TransferResponse serviceResponse = TransferResponse.builder()
                .status("APPROVED")
                .sourceAccount("001-100001")
                .targetAccount("001-200001")
                .amount(new BigDecimal("250.00"))
                .build();
        when(transferService.validateAndTransfer(any(TransferRequest.class)))
                .thenReturn(Mono.just(serviceResponse));

        // Act
        webTestClient.post()
                .uri("/api/transfers/control")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(expectedRequest)
                .exchange()

                // Assert
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.sourceAccount").isEqualTo("001-100001")
                .jsonPath("$.targetAccount").isEqualTo("001-200001")
                .jsonPath("$.amount").isEqualTo(250.00);

        ArgumentCaptor<TransferRequest> requestCaptor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferService, times(1)).validateAndTransfer(requestCaptor.capture());
        assertRequestEquals(expectedRequest, requestCaptor.getValue());
    }

    // Verifica que un rechazo de negocio emitido por el servicio llegue como respuesta HTTP de error.
    @Test
    void shouldReturnServerErrorWhenSourceAccountIsInactive() {
        // Arrange
        TransferRequest expectedRequest = request();
        when(transferService.validateAndTransfer(any(TransferRequest.class)))
                .thenReturn(Mono.error(new IllegalStateException("Cuenta origen no esta activa")));

        // Act
        webTestClient.post()
                .uri("/api/transfers/control")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(expectedRequest)
                .exchange()

                // Assert
                .expectStatus().is5xxServerError();

        verifyServiceCalledOnceWith(expectedRequest);
    }

    // Verifica el manejo HTTP de un error cuando el importe excede el límite diario.
    @Test
    void shouldReturnServerErrorWhenDailyLimitIsExceeded() {
        // Arrange
        TransferRequest expectedRequest = request();
        when(transferService.validateAndTransfer(any(TransferRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Limite diario excedido")));

        // Act
        webTestClient.post()
                .uri("/api/transfers/control")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(expectedRequest)
                .exchange()

                // Assert
                .expectStatus().is5xxServerError();

        verifyServiceCalledOnceWith(expectedRequest);
    }

    private static TransferRequest request() {
        return TransferRequest.builder()
                .customerId("100")
                .sourceAccount("001-100001")
                .targetAccount("001-200001")
                .amount(new BigDecimal("250.00"))
                .build();
    }

    private void verifyServiceCalledOnceWith(TransferRequest expectedRequest) {
        ArgumentCaptor<TransferRequest> requestCaptor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferService, times(1)).validateAndTransfer(requestCaptor.capture());
        assertRequestEquals(expectedRequest, requestCaptor.getValue());
    }

    private static void assertRequestEquals(TransferRequest expected, TransferRequest actual) {
        assertEquals(expected.getCustomerId(), actual.getCustomerId());
        assertEquals(expected.getSourceAccount(), actual.getSourceAccount());
        assertEquals(expected.getTargetAccount(), actual.getTargetAccount());
        assertEquals(expected.getAmount(), actual.getAmount());
    }
}
