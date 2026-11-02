package com.demo.example.mstransfercontrol.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private Long id;

    @Column("account_number")
    private String accountNumber;

    @Column("customer_id")
    private String customerId;

    private String status; // ACTIVE | BLOCKED
}

