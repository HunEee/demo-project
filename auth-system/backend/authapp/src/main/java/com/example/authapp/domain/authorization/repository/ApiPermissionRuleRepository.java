package com.example.authapp.domain.authorization.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.authorization.entity.ApiPermissionRuleEntity;

public interface ApiPermissionRuleRepository extends JpaRepository<ApiPermissionRuleEntity, Long> {

    List<ApiPermissionRuleEntity> findByEnabledTrueOrderByHttpMethodAscSortOrderAscPathPatternDesc();

    List<ApiPermissionRuleEntity> findByHttpMethodAndEnabledTrueOrderBySortOrderAscPathPatternDesc(String httpMethod);

    boolean existsByHttpMethodAndPathPatternAndPermissionCode(String httpMethod, String pathPattern, String permissionCode);
}
