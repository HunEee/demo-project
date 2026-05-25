package com.example.authapp.global.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
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
        if (userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
            return;
        }

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(UserException::roleNotFound);

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
    
}
