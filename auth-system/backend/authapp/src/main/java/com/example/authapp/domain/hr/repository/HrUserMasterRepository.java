package com.example.authapp.domain.hr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.hr.entity.HrAccountStatus;
import com.example.authapp.domain.hr.entity.HrUserMasterEntity;

public interface HrUserMasterRepository extends JpaRepository<HrUserMasterEntity, Long> {

    Optional<HrUserMasterEntity> findByEmployeeNo(String employeeNo);

    Optional<HrUserMasterEntity> findByAccountUsername(String accountUsername);

    List<HrUserMasterEntity> findByAccountStatus(HrAccountStatus accountStatus);

    boolean existsByEmployeeNo(String employeeNo);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
