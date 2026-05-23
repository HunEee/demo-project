package com.example.authapp.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.authapp.application.user.dto.SignupRequest;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.event.UserSignedUpEvent;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void createsLocalUserWithEncodedPasswordAndRole() {
        SignupRequest request = new SignupRequest(
                "localUser",
                "Password1!",
                "local@example.com",
                "123456",
                "local",
                null
        );
        RoleEntity role = RoleEntity.builder().name("ROLE_USER").build();

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userCommandService.createUser(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(userCaptor.getValue().isSocial()).isFalse();
        assertThat(userCaptor.getValue().getRoles()).contains(role);
    }

    @Test
    void convertsDatabaseDuplicateEmailToUserException() {
        SignupRequest request = new SignupRequest(
                "localUser",
                "Password1!",
                "local@example.com",
                "123456",
                "local",
                null
        );
        RoleEntity role = RoleEntity.builder().name("ROLE_USER").build();

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> userCommandService.createUser(request))
                .isInstanceOf(UserException.class);
    }
    
    
}
