package com.example.authapp.domain.authorization.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentHistoryResponse;
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;
import com.example.authapp.domain.authorization.entity.RoleAssignmentHistoryEntity;
import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleAssignmentHistoryRepository;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.organization.entity.GroupEntity;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final RoleAssignmentHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<AdminRoleAssignmentHistoryResponse> history() {
        return historyRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleAssignmentHistoryEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(AdminRoleAssignmentHistoryResponse::from)
                .toList();
    }

    @Transactional
    public void assignUserRole(String username, AdminRoleAssignmentRequest request) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        RoleEntity role = findRole(request);
        requireSensitiveReason(role, request);
        if (!user.getRoles().contains(role)) {
            user.addRole(role);
            saveHistory("USER", username, defaultText(user.getNickname(), username), role, "ASSIGN", request);
        }
    }

    @Transactional
    public void revokeUserRole(String username, Long roleId, String reason) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (user.getRoles().contains(role)) {
            user.removeRole(role);
            saveHistory("USER", username, defaultText(user.getNickname(), username), role, "REVOKE", new AdminRoleAssignmentRequest(roleId, null, reason, null));
        }
    }

    @Transactional
    public void assignGroupRole(Long groupId, AdminRoleAssignmentRequest request) {
        GroupEntity group = groupRepository.findById(groupId).orElseThrow();
        RoleEntity role = findRole(request);
        requireSensitiveReason(role, request);
        if (!group.getRoles().contains(role)) {
            group.addRole(role);
            saveHistory("GROUP", String.valueOf(groupId), group.getName(), role, "ASSIGN", request);
        }
    }

    @Transactional
    public void revokeGroupRole(Long groupId, Long roleId, String reason) {
        GroupEntity group = groupRepository.findById(groupId).orElseThrow();
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (group.getRoles().contains(role)) {
            group.removeRole(role);
            saveHistory("GROUP", String.valueOf(groupId), group.getName(), role, "REVOKE", new AdminRoleAssignmentRequest(roleId, null, reason, null));
        }
    }

    private RoleEntity findRole(AdminRoleAssignmentRequest request) {
        if (request.roleId() != null) {
            return roleRepository.findById(request.roleId()).orElseThrow();
        }
        requireText(request.roleName(), "Role id or name is required.");
        return roleRepository.findByName(request.roleName()).orElseThrow();
    }

    private void requireSensitiveReason(RoleEntity role, AdminRoleAssignmentRequest request) {
        if (role.hasSensitivePermission() && (request.sensitiveReason() == null || request.sensitiveReason().isBlank())) {
            throw new IllegalArgumentException("Sensitive reason is required.");
        }
    }

    private void saveHistory(
            String targetType,
            String targetId,
            String targetName,
            RoleEntity role,
            String action,
            AdminRoleAssignmentRequest request
    ) {
        historyRepository.save(RoleAssignmentHistoryEntity.builder()
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .roleId(role.getId())
                .roleName(role.getName())
                .action(action)
                .actorUsername(currentActor())
                .reason(request.reason())
                .sensitive(role.hasSensitivePermission())
                .sensitiveReason(blankToNull(request.sensitiveReason()))
                .build());
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) return "UNKNOWN";
        return authentication.getName();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
