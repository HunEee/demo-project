package com.example.authapp.domain.admin.dto;

// 관리자 조치 사유를 감사 로그와 화면 표시용으로 전달하는 요청 DTO
public record AdminActionRequest(String reason) {
    public String normalizedReason() {
        return reason == null || reason.isBlank() ? "관리자 수동 조치" : reason;
    }
}
