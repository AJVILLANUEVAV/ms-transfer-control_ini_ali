package com.demo.example.mstransfercontrol.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    private Long id;

    @Column("account_number")
    private String accountNumber;

    @Column("account_destination")
    private String accountDestination;

    @Column("customer_id")
    private String customerId;

    private String status; // successful | rejected

    private BigDecimal amount;

    @Column("transfer_date")
    private LocalDateTime transferDate;

}
