package com.example.authapp.domain.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.organization.entity.GroupEntity;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
	
}
