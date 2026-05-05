package com.example.authapp.global.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.authapp.domain.jwt.repository.RefreshTokenRepository;
import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.repository.RiskRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleConfig {

    private final RefreshTokenRepository refreshRepository;
    private final RiskRepository riskRepository;
    
    
    // 매일 새벽 4시 위험 점수 자동 회복
    @Scheduled(cron = "0 0 4 * * *")
    public void riskRecoverySchedule() {
        List<RiskEntity> risks = riskRepository.findAll();
        for (RiskEntity risk : risks) {
            if (risk.getRiskScore() > 0) {
                risk.decreaseRisk(5, "TIME_DECAY");
            }
        }
        riskRepository.saveAll(risks);
    }
    
    

//    // Refresh 토큰 저장소 8일 지난 토큰 삭제
//    @Scheduled(cron = "0 0 3 * * *")
//    public void refreshEntityTtlSchedule() {
//        LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
//        refreshRepository.deleteByCreatedDateBefore(cutoff);
//    }

}
