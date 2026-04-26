package com.example.authapp.domain.verificatoin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.verificatoin.entity.EmailTokenEntity;

public interface EmailTokenRepository extends JpaRepository<EmailTokenEntity, Long> {

    Optional<EmailTokenEntity> findByToken(String token);
    
}
