package com.demo.example.mstransfercontrol.service;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import reactor.core.publisher.Mono;

public interface TransferService {
    Mono<TransferResponse> validateAndTransfer(TransferRequest request);
}
