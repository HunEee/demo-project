package com.example.authapp.domain.verificatoin.entity;

import java.time.LocalDateTime;

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
@Table(name = "email_code_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String code;

    @Enumerated(EnumType.STRING)
    private EmailCodePurpose purpose;

    private boolean used;

    private LocalDateTime expiredAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public void use() {
        this.used = true;
    }
	
}
