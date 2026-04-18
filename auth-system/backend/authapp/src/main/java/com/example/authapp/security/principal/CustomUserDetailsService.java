package com.example.authapp.security.principal;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // 자체 로그인
    // DB에 저장된 username/password를 가져온다.
    @Override
    public UserPrincipal loadUserByUsername(String username) {
        UserEntity entity = userRepository.findWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저 없음"));

        return new UserPrincipal(entity);
    }
    
    
}