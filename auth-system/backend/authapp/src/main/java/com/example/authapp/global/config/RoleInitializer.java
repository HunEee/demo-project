package com.example.authapp.global.config;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.repository.RoleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;

    // ROLE 추가되면 자동 생성
    @PostConstruct
    public void init() {
        for (UserRoleType role : UserRoleType.values()) {
            if (roleRepository.findByName(role.name()).isEmpty()) {
                roleRepository.save(
                        RoleEntity.builder()
                        			.name(role.name())
                        			.build()
                );
            }
        }
        
    }
    
    
}
