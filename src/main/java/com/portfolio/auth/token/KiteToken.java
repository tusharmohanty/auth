package com.portfolio.auth.token;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "kite_tokens")
@Data
public class KiteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false)
    private String accessToken;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;
}