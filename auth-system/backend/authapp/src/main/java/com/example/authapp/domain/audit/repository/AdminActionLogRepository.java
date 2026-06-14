package com.example.authapp.domain.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLogEntity, Long> {

    List<AdminActionLogEntity> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);

    List<AdminActionLogEntity> findByActorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(String actorUsername);

    @Query("""
            select log
            from AdminActionLogEntity log
            where upper(log.actorUsername) <> 'SYSTEM'
              and (:actor is null or lower(log.actorUsername) like lower(concat('%', :actor, '%')))
              and (
                  :target is null
                  or lower(log.targetType) like lower(concat('%', :target, '%'))
                  or lower(log.targetId) like lower(concat('%', :target, '%'))
                  or lower(log.targetUsername) like lower(concat('%', :target, '%'))
                  or lower(log.targetName) like lower(concat('%', :target, '%'))
              )
              and (:actionType is null or log.actionType = :actionType)
              and (:result is null or lower(log.result) like lower(concat('%', :result, '%')))
              and (:reason is null or lower(log.reason) like lower(concat('%', :reason, '%')))
              and (:riskLevel is null or lower(log.riskLevel) like lower(concat('%', :riskLevel, '%')))
              and (:ipAddress is null or lower(log.ipAddress) like lower(concat('%', :ipAddress, '%')))
              and (
                  :userAgent is null
                  or lower(log.userAgent) like lower(concat('%', :userAgent, '%'))
                  or lower(log.device) like lower(concat('%', :userAgent, '%'))
              )
              and (:from is null or log.createdAt >= :from)
              and (:to is null or log.createdAt <= :to)
            """)
    Page<AdminActionLogEntity> search(
            @Param("actor") String actor,
            @Param("target") String target,
            @Param("actionType") AdminActionType actionType,
            @Param("result") String result,
            @Param("reason") String reason,
            @Param("riskLevel") String riskLevel,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
    
}
