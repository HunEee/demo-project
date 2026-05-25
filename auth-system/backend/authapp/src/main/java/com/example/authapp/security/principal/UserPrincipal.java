package com.example.authapp.security.principal;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.authapp.domain.user.entity.UserEntity;

import lombok.Getter;

/**
 * Spring Security 인증 객체(UserDetails)
 *
 * 역할:
 * - 로그인 사용자 정보를 SecurityContext 에 저장
 * - 인증/인가 시 사용자 정보 제공
 * - JWT 기반 인증 시 Authentication Principal 로 사용
 *
 * Spring Security 내부에서 사용하는 "인증된 사용자 객체"
 */
@Getter
public class UserPrincipal implements UserDetails {

    // 사용자 PK -> DB 식별용, JWT 생성 시 userId claim 으로 사용 가능
    private final Long userId;

    //로그인 아이디(username) -> Spring Security 의 기본 식별값
    private final String username;

    //암호화된 비밀번호 -> JWT 인증 시에는 잘 사용하지 않지만 Form Login / DaoAuthenticationProvider 에서 사용 가능
    private final String password;

    // 계정 활성화 여부 -> false 면 로그인 차단 가능
    private final boolean enabled;

    // 계정 잠금 여부 (true = 잠기지 않음, false = 잠김) -> user.isLocked() 값을 반대로 저장
    private final boolean accountNonLocked;

    // 사용자 권한 목록(ROLE_USER, ROLE_ADMIN)
    private final Collection<? extends GrantedAuthority> authorities;

    
    /**
     * DB UserEntity 기반 생성자 
     * - 로그인 시 DB 조회 후 사용하는 생성자
     * - UserEntity -> UserPrincipal 변환
     */
    public UserPrincipal(UserEntity user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.accountNonLocked = !user.isLocked();
        //RoleEntity -> GrantedAuthority 변환(Spring Security 는 권한 객체를 사용하므로 ROLE_USER 문자열을 SimpleGrantedAuthority 로 변환)
        this.authorities = user.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority(role.getName()))
                                .collect(Collectors.toSet());
    }

    /**
     * JWT claims 기반 생성자
     * - DB 조회 없이 JWT 안의 정보만으로 인증 객체 생성 가능
     * - 사용 예: JWTFilter, Stateless 인증
     */
    public UserPrincipal(String username, Collection<? extends GrantedAuthority> authorities) {
        this.userId = null;
        // JWT 에서 추출한 username
        this.username = username;
        this.password = "";
        //JWT 자체가 유효하다고 가정 -> DB 상태(enabled/locked) 즉시 반영 어려움
        this.enabled = true;
        this.accountNonLocked = true;
        // JWT claims 에서 추출한 권한 저장
        this.authorities = Set.copyOf(authorities);
    }

    // 사용자 권한 반환 -> Spring Security 인가(authorize) 시 사용
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 계정 잠금 여부 반환(true  = 잠기지 않음, false = 잠김)
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    // 계정 활성화 여부 반환
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // 계정 만료 여부(true = 만료되지 않음)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 비밀번호 만료 여부(true = 만료되지 않음)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

}