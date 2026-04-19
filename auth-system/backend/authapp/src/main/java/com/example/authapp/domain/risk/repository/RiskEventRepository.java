package com.example.authapp.domain.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.risk.entity.RiskEventEntity;

public interface RiskEventRepository extends JpaRepository<RiskEventEntity, Long> {

}
