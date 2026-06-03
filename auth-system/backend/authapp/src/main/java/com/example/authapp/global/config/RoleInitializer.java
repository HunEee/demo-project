package com.example.authapp.global.config;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.entity.UserRoleType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        Map<String, RoleEntity> existingRoles = roleRepository.findByNameIn(
                        Arrays.stream(UserRoleType.values())
                                .map(Enum::name)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(RoleEntity::getName, role -> role));

        for (UserRoleType role : UserRoleType.values()) {
            RoleEntity existingRole = existingRoles.get(role.name());
            if (existingRole == null) {
                roleRepository.save(
                        RoleEntity.builder()
                                .name(role.name())
                                .displayName(role.getLabel())
                                .enabled(true)
                                .systemRole(true)
                                .build()
                );
                continue;
            }

            if (!existingRole.isEnabled()
                    || !existingRole.isSystemRole()
                    || !role.getLabel().equals(existingRole.getDisplayName())) {
                existingRole.update(
                        existingRole.getName(),
                        role.getLabel(),
                        existingRole.getDescription(),
                        true,
                        true
                );
                roleRepository.save(existingRole);
            }
        }
    }
}
