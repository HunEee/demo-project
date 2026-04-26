package com.example.authapp.domain.verificatoin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_token_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(unique = true, nullable = false, length = 200)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose; 

    private boolean used;

    private LocalDateTime expiredAt;

    public void use() {
        this.used = true;
    }

    public boolean isExpired() {
        return expiredAt.isBefore(LocalDateTime.now());
    }
    
} 
