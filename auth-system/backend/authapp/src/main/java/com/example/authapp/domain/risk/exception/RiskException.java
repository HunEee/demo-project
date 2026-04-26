package com.example.authapp.domain.risk.exception;

import com.example.authapp.global.exception.CustomException;

public class RiskException extends CustomException {

	public RiskException(int status, String message) {
		super(status, message);
	}
	
    // 사용자 없음
    public static RiskException userNotFound() {
        return new RiskException(404, "사용자를 찾을 수 없습니다.");
    }

    // 재사용된 Refresh Token
    public static RiskException tokenReuseDetected() {
        return new RiskException(401, "재사용된 Refresh Token 입니다.");
    }

    // 만료된 Refresh Token 재사용
    public static RiskException expiredTokenDetected() {
        return new RiskException(401, "만료된 Refresh Token 입니다.");
    }

    // 위험 점수 높아 차단
    public static RiskException highRiskTokenBlocked() {
        return new RiskException(403, "위험도가 높은 토큰 접근이 차단되었습니다.");
    }

    // 의심 로그인 차단
    public static RiskException suspiciousLoginBlocked() {
        return new RiskException(403, "의심스러운 로그인으로 접근이 차단되었습니다.");
    }

    // Risk Entity 없음
    public static RiskException riskNotFound() {
        return new RiskException(404, "위험 정보가 존재하지 않습니다.");
    }

    // 내부 처리 실패
    public static RiskException riskProcessFailed() {
        return new RiskException(500, "위험도 처리 중 오류가 발생했습니다.");
    }
	

}
