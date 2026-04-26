package com.example.authapp.domain.verificatoin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.verificatoin.entity.EmailCodeEntity;
import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;

public interface EmailCodeRepository extends JpaRepository<EmailCodeEntity, Long> {

    Optional<EmailCodeEntity> findTopByEmailAndPurposeOrderByIdDesc(String email, EmailCodePurpose purpose);
    
}
