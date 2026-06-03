package com.example.authapp.domain.organization.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;
import com.example.authapp.domain.audit.entity.AdminActionType;
import com.example.authapp.domain.audit.repository.AdminActionLogRepository;
import com.example.authapp.domain.authorization.dto.AdminRoleAssignmentRequest;
import com.example.authapp.domain.authorization.service.RoleAssignmentService;
import com.example.authapp.domain.organization.dto.AdminGroupDetailResponse;
import com.example.authapp.domain.organization.dto.AdminGroupMemberRequest;
import com.example.authapp.domain.organization.dto.AdminGroupMemberResponse;
import com.example.authapp.domain.organization.dto.AdminGroupRequest;
import com.example.authapp.domain.organization.dto.AdminGroupResponse;
import com.example.authapp.domain.organization.dto.AdminGroupRoleRequest;
import com.example.authapp.domain.organization.dto.AdminGroupRoleResponse;
import com.example.authapp.domain.organization.entity.GroupEntity;
import com.example.authapp.domain.organization.entity.GroupUserEntity;
import com.example.authapp.domain.organization.repository.GroupRepository;
import com.example.authapp.domain.organization.repository.GroupUserRepository;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final AdminActionLogRepository adminActionLogRepository;

    @Transactional(readOnly = true)
    public List<AdminGroupResponse> list() {
        return groupRepository.findAll().stream()
                .sorted(Comparator.comparing(GroupEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminGroupDetailResponse detail(Long id) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        List<AdminGroupMemberResponse> members = groupUserRepository.findByGroupId(id).stream()
                .map(member -> AdminGroupMemberResponse.from(member, findUser(member.getUsername())))
                .toList();
        List<AdminGroupRoleResponse> roles = group.getRoles().stream()
                .sorted(Comparator.comparing(role -> role.getName().toLowerCase()))
                .map(AdminGroupRoleResponse::from)
                .toList();
        return new AdminGroupDetailResponse(toResponse(group), members, roles);
    }

    @Transactional
    public AdminGroupResponse create(AdminGroupRequest request) {
        requireText(request.name(), "Group name is required.");
        if (groupRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Group name already exists.");
        }
        validateOwner(request.ownerUsername());
        GroupEntity group = GroupEntity.builder()
                .name(request.name())
                .type(defaultText(request.type(), "STATIC"))
                .ownerUsername(blankToNull(request.ownerUsername()))
                .description(request.description())
                .enabled(request.enabled() == null || request.enabled())
                .build();

        GroupEntity saved = groupRepository.save(group);
        saveAudit(saved, null, groupState(saved), AdminActionType.GROUP_CREATE, request.reason());
        return toResponse(saved);
    }

    @Transactional
    public AdminGroupResponse update(Long id, AdminGroupRequest request) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        String before = groupState(group);
        validateOwner(request.ownerUsername());
        group.update(
                defaultText(request.name(), group.getName()),
                defaultText(request.type(), group.getType()),
                blankToNull(request.ownerUsername()),
                request.description(),
                request.enabled() == null ? group.isEnabled() : request.enabled()
        );
        saveAudit(group, before, groupState(group), AdminActionType.GROUP_UPDATE, request.reason());
        return toResponse(group);
    }

    @Transactional
    public void disable(Long id, String reason) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        String before = groupState(group);
        group.disable();
        saveAudit(group, before, groupState(group), AdminActionType.GROUP_DISABLE, reason);
    }

    @Transactional
    public AdminGroupDetailResponse addMember(Long id, AdminGroupMemberRequest request) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        requireText(request.username(), "Username is required.");
        userRepository.findByUsername(request.username()).orElseThrow();
        if (!groupUserRepository.existsByGroupIdAndUsername(id, request.username())) {
            groupUserRepository.save(GroupUserEntity.builder()
                    .group(group)
                    .username(request.username())
                    .build());
            saveAudit(group, null, "{\"username\":\"" + safe(request.username()) + "\"}", AdminActionType.GROUP_MEMBER_ADD, request.reason());
        }
        return detail(id);
    }

    @Transactional
    public AdminGroupDetailResponse removeMember(Long id, String username, String reason) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        groupUserRepository.findByGroupIdAndUsername(id, username).ifPresent(groupUserRepository::delete);
        saveAudit(group, "{\"username\":\"" + safe(username) + "\"}", null, AdminActionType.GROUP_MEMBER_REMOVE, reason);
        return detail(id);
    }

    @Transactional
    public AdminGroupDetailResponse assignRole(Long id, AdminGroupRoleRequest request) {
        requireText(request.roleName(), "Role name is required.");
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        roleAssignmentService.assignGroupRole(id, new AdminRoleAssignmentRequest(null, request.roleName(), request.reason(), request.sensitiveReason()));
        saveAudit(group, null, "{\"role\":\"" + safe(request.roleName()) + "\"}", AdminActionType.GROUP_ROLE_ASSIGN, request.reason());
        return detail(id);
    }

    @Transactional
    public AdminGroupDetailResponse removeRole(Long id, Long roleId, String reason) {
        GroupEntity group = groupRepository.findById(id).orElseThrow();
        roleAssignmentService.revokeGroupRole(id, roleId, reason);
        saveAudit(group, "{\"roleId\":\"" + roleId + "\"}", null, AdminActionType.GROUP_ROLE_REMOVE, reason);
        return detail(id);
    }

    private AdminGroupResponse toResponse(GroupEntity group) {
        Long groupId = group.getId();
        long userCount = groupId == null ? 0 : groupUserRepository.countByGroupId(groupId);
        long roleCount = group.getRoles().size();
        return AdminGroupResponse.from(group, userCount, roleCount);
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private void validateOwner(String ownerUsername) {
        if (ownerUsername == null || ownerUsername.isBlank()) return;
        userRepository.findByUsername(ownerUsername).orElseThrow();
    }

    private void saveAudit(GroupEntity group, String beforeValue, String afterValue, AdminActionType actionType, String reason) {
        adminActionLogRepository.save(AdminActionLogEntity.builder()
                .actorUsername(currentActor())
                .targetType("GROUP")
                .targetId(group.getId() != null ? String.valueOf(group.getId()) : group.getName())
                .targetName(group.getName())
                .actionType(actionType)
                .reason(reason)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress("UNKNOWN")
                .device("UNKNOWN")
                .build());
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) return "UNKNOWN";
        return authentication.getName();
    }

    private String groupState(GroupEntity group) {
        return "{\"id\":\"" + (group.getId() != null ? group.getId() : group.getName())
                + "\",\"type\":\"" + safe(group.getType())
                + "\",\"enabled\":" + group.isEnabled()
                + ",\"ownerUsername\":\"" + safe(group.getOwnerUsername())
                + "\"}";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
}
