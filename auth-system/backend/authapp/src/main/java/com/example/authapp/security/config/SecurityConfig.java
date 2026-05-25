package com.example.authapp.security.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.domain.jwt.service.CookieService;
import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.security.filter.JWTFilter;
import com.example.authapp.security.filter.LoginFilter;
import com.example.authapp.security.handler.RefreshTokenLogoutHandler;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
    @Value("${api.prefix}")
    private String API_PREFIX;
	
	// 인증을 수행
    private final AuthenticationConfiguration authenticationConfiguration;
    // 로그인 성공 이후 로직(로그인 성공 핸들러)
    private final AuthenticationSuccessHandler loginSuccessHandler;
    // 로그인 실패 핸들러
    private final AuthenticationFailureHandler loginFailureHandler;
    // 소셜 로그인 성공 핸들러
    private final AuthenticationSuccessHandler socialSuccessHandler;
    
    // 로그아웃 핸들러에 주입용
    //private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    // 쿠키 주입
    private final CookieService cookieService;
    // 로그인 기록 남기기
    private final LoginHistoryService loginHistoryService;
    // 이벤트 기록
    private final AuthEventLogService securityEventService;
    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("loginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
            @Qualifier("socialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
            AuthenticationFailureHandler loginFailureHandler,
            //JwtService jwtService,
            RefreshTokenService refreshTokenService,
            CookieService cookieService,
            LoginHistoryService loginHistoryService,
            AuthEventLogService securityEventService
    ) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        //this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.loginHistoryService = loginHistoryService;
        this.securityEventService = securityEventService;
    }
    

    //****************************************************************************
    // 빈 등록
    //****************************************************************************
    
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

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
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
        return RoleHierarchyImpl.withRolePrefix("")
				                .role(UserRoleType.ROLE_ADMIN.name())
				                .implies(UserRoleType.ROLE_USER.name())
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

        // OAuth2
        http.oauth2Login(oauth2 -> oauth2.successHandler(socialSuccessHandler));

        // 인가
        http.authorizeHttpRequests(auth -> auth
                // 내부 에러 처리 요청이 인증 필터에 막히지 않도록 정리
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
        		// 인증관련
                .requestMatchers(API_PREFIX + "/jwt/exchange", API_PREFIX + "/jwt/refresh").permitAll()
                // 비인증 API
                .requestMatchers(HttpMethod.POST,
                    API_PREFIX + "/users",
                    API_PREFIX + "/users/exists",
                    API_PREFIX + "/users/find-username",
                    API_PREFIX + "/users/password/reset",
                    API_PREFIX + "/verification/**",
                    API_PREFIX + "/login"
                ).permitAll()
                // 인증 필요
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.PATCH, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, API_PREFIX + "/users/me/password").hasRole("USER")
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/security").hasRole("USER")
                // 관리자
                .requestMatchers(API_PREFIX + "/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/users").hasRole("ADMIN")
                .anyRequest().authenticated()
        );

        // 기본 로그아웃 필터 + 커스텀 Refresh 토큰 삭제 핸들러 추가
        http.logout(logout -> logout
        		.logoutUrl(API_PREFIX + "/logout")
                .addLogoutHandler(new RefreshTokenLogoutHandler(refreshTokenService, cookieService, loginHistoryService,securityEventService))
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                })
        );

        // 예외 처리
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)) // 401 응답(로그인을 안한상태)
                .accessDeniedHandler((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN)) // 403 응답(로그인을 했지만 권한이 없는 상태)
        );

        // 로그인 필터
        http.addFilterBefore(
                new LoginFilter(authenticationManager(authenticationConfiguration),
                        loginSuccessHandler,
                        loginFailureHandler,
                        API_PREFIX ),
                UsernamePasswordAuthenticationFilter.class
        );

        // JWT 필터 추가
        http.addFilterBefore(new JWTFilter(), LogoutFilter.class);

        // 세션 필터 설정 (STATELESS)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
    
    
}
