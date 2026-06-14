package com.example.authapp.application.admin.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.authapp.domain.hr.dto.HrUserMasterRequest;
import com.example.authapp.domain.hr.dto.HrUserMasterResponse;
import com.example.authapp.domain.hr.service.HrUserMasterService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHrUserMasterUseCase {

    private final HrUserMasterService service;

    public List<HrUserMasterResponse> list(String keyword, String accountStatus) {
        return service.list(keyword, accountStatus);
    }

    public List<HrUserMasterResponse> accountCandidates() {
        return service.accountCandidates();
    }

    public boolean exists(String field, String value) {
        return service.exists(field, value);
    }

    public HrUserMasterResponse create(HrUserMasterRequest request) {
        return service.create(request);
    }

    public HrUserMasterResponse update(Long id, HrUserMasterRequest request) {
        return service.update(id, request);
    }

    public void delete(Long id) {
        service.delete(id);
    }
}
