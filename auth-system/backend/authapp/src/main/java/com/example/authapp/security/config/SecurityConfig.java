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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.authapp.domain.audit.service.LoginHistoryService;
import com.example.authapp.domain.audit.service.AuthEventLogService;
import com.example.authapp.application.auth.usecase.CookieService;
import com.example.authapp.domain.jwt.service.JwtTokenProvider;
import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.system.settings.service.WebSecuritySettingsService;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.security.filter.JWTFilter;
import com.example.authapp.security.filter.LoginFilter;
import com.example.authapp.security.handler.RefreshTokenLogoutHandler;
import com.example.authapp.security.rbac.RbacAuthorizationManager;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
    @Value("${api.prefix}")
    private String API_PREFIX;
	
	// 인증 설정을 위임한다.
    private final AuthenticationConfiguration authenticationConfiguration;
    // 로그인 성공 후속 로직을 처리한다.
    private final AuthenticationSuccessHandler loginSuccessHandler;
    // 로그인 실패 후속 로직을 처리한다.
    private final AuthenticationFailureHandler loginFailureHandler;
    // 소셜 로그인 성공 후속 로직을 처리한다.
    private final AuthenticationSuccessHandler socialSuccessHandler;
    
    // 로그아웃 핸들러에 주입한다.
    private final RefreshTokenService refreshTokenService;
    // 쿠키 처리를 위임한다.
    private final CookieService cookieService;
    // 로그인 이력 기록을 위임한다.
    private final LoginHistoryService loginHistoryService;
    // 보안 이벤트 기록을 위임한다.
    private final AuthEventLogService securityEventService;
    private final RbacAuthorizationManager rbacAuthorizationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSecuritySettingsService webSecuritySettingsService;
    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("loginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
            @Qualifier("socialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
            AuthenticationFailureHandler loginFailureHandler,
            RefreshTokenService refreshTokenService,
            CookieService cookieService,
            LoginHistoryService loginHistoryService,
            AuthEventLogService securityEventService,
            RbacAuthorizationManager rbacAuthorizationManager,
            JwtTokenProvider jwtTokenProvider,
            WebSecuritySettingsService webSecuritySettingsService
    ) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.loginHistoryService = loginHistoryService;
        this.securityEventService = securityEventService;
        this.rbacAuthorizationManager = rbacAuthorizationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.webSecuritySettingsService = webSecuritySettingsService;
    }
    

    //****************************************************************************
    // 빈 등록
    //****************************************************************************
    
    // 비밀번호 암호화에 사용할 BCrypt 인코더를 등록한다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 커스텀 로그인 필터에서 사용할 AuthenticationManager를 등록한다.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // CORS 설정을 등록한다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var policy = webSecuritySettingsService.current();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(csv(policy.getAllowedOrigins()));
        configuration.setAllowedMethods(csv(policy.getAllowedMethods()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(policy.isAllowCredentials());
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 역할 계층을 등록한다.
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("")
				                .role(UserRoleType.ROLE_ADMIN.name())
				                .implies(UserRoleType.ROLE_USER.name())
				                .build();
    }

    // SecurityFilterChain을 구성한다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // STATELESS 인증 구조에 맞게 CSRF를 비활성화한다.
        http.csrf(AbstractHttpConfigurer::disable);
        // CORS 정책을 적용한다.
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        // 기본 폼 로그인 필터를 비활성화한다.
        http.formLogin(AbstractHttpConfigurer::disable);
        // 기본 Basic 인증 필터를 비활성화한다.
        http.httpBasic(AbstractHttpConfigurer::disable);

        // OAuth2
        http.oauth2Login(oauth2 -> oauth2.successHandler(socialSuccessHandler));

        // ?멸?
        http.authorizeHttpRequests(auth -> auth
                // 내부 오류 처리 요청은 인증 필터에 막히지 않게 허용한다.
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
        		// 인증 관련 공개 API를 허용한다.
                .requestMatchers(API_PREFIX + "/jwt/exchange", API_PREFIX + "/jwt/refresh").permitAll()
                // 비인증 API를 허용한다.
                .requestMatchers(HttpMethod.POST,
                    API_PREFIX + "/users",
                    API_PREFIX + "/users/exists",
                    API_PREFIX + "/users/find-username",
                    API_PREFIX + "/users/password/reset",
                    API_PREFIX + "/verification/**",
                    API_PREFIX + "/auth/mfa/verify",
                    API_PREFIX + "/auth/mfa/totp/setup",
                    API_PREFIX + "/auth/mfa/totp/confirm",
                    API_PREFIX + "/auth/login",
                    API_PREFIX + "/login"
                ).permitAll()
                // 인증이 필요한 사용자 API를 제한한다.
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.PATCH, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, API_PREFIX + "/users/me").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, API_PREFIX + "/users/me/password").hasRole("USER")
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/security").hasRole("USER")
                // 관리자 API는 RBAC 인가를 적용한다.
                .requestMatchers(API_PREFIX + "/admin/**").access(rbacAuthorizationManager)
                .requestMatchers(HttpMethod.GET, API_PREFIX + "/users").access(rbacAuthorizationManager)
                .anyRequest().authenticated()
        );

        // 기본 로그아웃 처리에 refresh token 폐기 핸들러를 추가한다.
        var requestMatcherBuilder = PathPatternRequestMatcher.withDefaults();
        http.logout(logout -> logout
        		.logoutRequestMatcher(new OrRequestMatcher(
                        requestMatcherBuilder.matcher(HttpMethod.POST, API_PREFIX + "/auth/logout"),
                        requestMatcherBuilder.matcher(HttpMethod.POST, API_PREFIX + "/logout")
                ))
                .addLogoutHandler(new RefreshTokenLogoutHandler(refreshTokenService, cookieService, loginHistoryService,securityEventService))
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                })
        );

        // 인증과 인가 예외 응답을 설정한다.
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)) // 로그인하지 않은 상태는 401로 응답한다.
                .accessDeniedHandler((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN)) // 로그인했지만 권한이 없으면 403으로 응답한다.
        );

        // 로그인 필터를 등록한다.
        http.addFilterBefore(
                new LoginFilter(authenticationManager(authenticationConfiguration),
                        loginSuccessHandler,
                        loginFailureHandler,
                        API_PREFIX ),
                UsernamePasswordAuthenticationFilter.class
        );

        // JWT 필터를 등록한다.
        http.addFilterBefore(new JWTFilter(refreshTokenService, jwtTokenProvider), LogoutFilter.class);

        // 세션 정책을 STATELESS로 설정한다.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    private List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
    
    
}
