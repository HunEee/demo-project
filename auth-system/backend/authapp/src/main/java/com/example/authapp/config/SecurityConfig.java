package com.example.authapp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.filter.JWTFilter;
import com.example.authapp.filter.LoginFilter;
import com.example.authapp.handler.RefreshTokenLogoutHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	// 인증을 수행
    private final AuthenticationConfiguration authenticationConfiguration;
	// 로그인 성공 이후 로직(로그인 성공 핸들러)
    private final AuthenticationSuccessHandler loginSuccessHandler;
    // 소셜 로그인 핸들러
    private final AuthenticationSuccessHandler socialSuccessHandler;
    // 로그아웃 핸들러에 주입용
    private final JwtService jwtService;
    
    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
            @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
            JwtService jwtService
    ) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.jwtService = jwtService;
    }
    
    
    // 비밀번호 단방향(BCrypt) 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 커스텀 자체 로그인 필터를 위한 AuthenticationManager Bean 수동 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    
    //CORS 빈
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    // 권한 계층
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                				.role(UserRoleType.ADMIN.name())
                				.implies(UserRoleType.USER.name())
                				.build();
    }
    
    
    // SecurityFilterChain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // CSRF 보안 필터 disable -> STATELESS의 경우 disable
        http.csrf(AbstractHttpConfigurer::disable);
        
        // CORS 설정
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
        // 기본 Form 기반 인증 필터들 disable
        http.formLogin(AbstractHttpConfigurer::disable);

        // 기본 Basic 인증 필터 disable
        http.httpBasic(AbstractHttpConfigurer::disable);

        // OAuth2 인증용
        http.oauth2Login(oauth2 -> oauth2.successHandler(socialSuccessHandler));
        
        // 인가
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/exist", "/user").permitAll()
                .requestMatchers(HttpMethod.GET, "/user").hasRole(UserRoleType.USER.name())
                .requestMatchers(HttpMethod.PUT, "/user").hasRole(UserRoleType.USER.name())
                .requestMatchers(HttpMethod.DELETE, "/user").hasRole(UserRoleType.USER.name())
                .anyRequest().authenticated()
        );

        // 기본 로그아웃 필터 + 커스텀 Refresh 토큰 삭제 핸들러 추가
        http.logout(logout -> logout.addLogoutHandler(new RefreshTokenLogoutHandler(jwtService)));
        
        // 예외 처리
        http.exceptionHandling(
        		e -> e.authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답(로그인을 안한상태)
                      })
                      .accessDeniedHandler((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답(로그인을 했지만 권한이 없는 상태)
                      })
        );
        
        // 커스텀 필터 추가
        http.addFilterBefore(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new JWTFilter(), LogoutFilter.class);
        
        // 세션 필터 설정 (STATELESS)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
    

}
