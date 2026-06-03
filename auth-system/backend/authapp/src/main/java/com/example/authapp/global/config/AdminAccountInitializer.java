package com.example.authapp.global.config;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.authorization.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.authorization.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(20)
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_NICKNAME = "Administrator";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Optional<UserEntity> existingAdmin = userRepository.findByUsername(ADMIN_USERNAME);
        if (existingAdmin.isPresent()) {
            UserEntity admin = existingAdmin.get();
            if (admin.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
                admin.addRole(findAdminRole());
            }
            return;
        }

        RoleEntity adminRole = findAdminRole();
        UserEntity admin = UserEntity.builder()
                .username(ADMIN_USERNAME)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .email(ADMIN_EMAIL)
                .nickname(ADMIN_NICKNAME)
                .profileImage(null)
                .locked(false)
                .enabled(true)
                .social(false)
                .socialProviderType(null)
                .providerId(null)
                .build();

        admin.addRole(adminRole);
        userRepository.save(admin);
    }

    private RoleEntity findAdminRole() {
        return roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(UserException::roleNotFound);
    }
    
}
