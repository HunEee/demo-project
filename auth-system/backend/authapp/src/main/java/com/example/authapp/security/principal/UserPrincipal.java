package com.example.authapp.security.principal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.authapp.domain.user.entity.UserEntity;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;

    private final boolean enabled;
    private final boolean accountNonLocked;

    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UserEntity user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();

        this.enabled = user.isEnabled();
        this.accountNonLocked = !user.isLocked();

        this.authorities = user.getRoles()
		                        .stream()
		                        .map(role -> new SimpleGrantedAuthority(role.getName()))
		                        .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
}
