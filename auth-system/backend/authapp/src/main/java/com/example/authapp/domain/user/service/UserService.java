package com.example.authapp.domain.user.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.jwt.service.JwtService;
import com.example.authapp.domain.user.dto.CustomOAuth2User;
import com.example.authapp.domain.user.dto.UserRequestDTO;
import com.example.authapp.domain.user.dto.UserResponseDTO;
import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.SocialProviderType;
import com.example.authapp.domain.user.entity.UserEntity;
import com.example.authapp.domain.user.entity.UserRoleType;
import com.example.authapp.domain.user.exception.UserException;
import com.example.authapp.domain.user.repository.RoleRepository;
import com.example.authapp.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
	private final JwtService jwtService;
	
    // 자체 로그인 회원 가입 (존재 여부)
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }
    
    // 자체 로그인 회원 가입
    @Transactional
    public Long addUser(UserRequestDTO dto) {

    	if (userRepository.existsByUsername(dto.getUsername())) {
    	    throw UserException.duplicateUsername();
    	}

    	if (userRepository.existsByEmail(dto.getEmail())) {
    	    throw UserException.duplicateEmail();
    	}

    	RoleEntity userRole = roleRepository.findByName("ROLE_USER")
    	        .orElseThrow(UserException::roleNotFound);
        
        UserEntity user = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .profileImage(dto.getProfileImage())
                .locked(false)
                .enabled(true)
                .isSocial(false)
                .socialProviderType(null)
                .providerId(null)
                .build();
        
        user.addRole(userRole);
        
        return userRepository.save(user).getId();
    }    
    
    // 자체 로그인
    // DB에 저장된 username/password를 가져온다.
    @Transactional(readOnly = true)
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	    UserEntity entity = userRepository.findByUsernameAndLockedAndIsSocial(username, false, false)
	    		.orElseThrow(() -> new UsernameNotFoundException("유저가 존재하지 않습니다."));

	    return User.builder()
	            .username(entity.getUsername())
	            .password(entity.getPassword())
	            .authorities(
	            	    entity.getRoles().stream()
	            	        .map(role -> new SimpleGrantedAuthority(role.getName()))
	            	        .toList()
	            )
	            .accountLocked(entity.getLocked())
	            .disabled(!entity.getEnabled())
	            .build();
	}
    
    // 자체 로그인 회원 정보 수정
    @Transactional
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인만 수정 가능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessionUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }
        
        // 중복 이메일 검증
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 이메일이 존재합니다.");
        }
        
        // 조회
        UserEntity entity = userRepository.findByUsernameAndLockedAndIsSocial(dto.getUsername(), false, false)
                							.orElseThrow(() -> new UsernameNotFoundException(dto.getUsername()));

        // 회원 정보 수정
        entity.updateUser(dto);

        return userRepository.save(entity).getId();
    }
    
    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {

        // 본인 및 어드민만 삭제 가능 검증
        SecurityContext context = SecurityContextHolder.getContext();
        String sessionUsername = context.getAuthentication().getName();
        String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessionUsername.equals(dto.getUsername());
        boolean isAdmin = sessionRole.equals("ROLE_"+UserRoleType.ROLE_ADMIN.name());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        // 유저 제거
        userRepository.deleteByUsername(dto.getUsername());

        // Refresh 토큰 제거
        jwtService.removeRefreshUser(dto.getUsername());
    }
    
    
    // 소셜 로그인 (매 로그인시 : 신규 = 가입, 기존 = 업데이트)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 부모 메소드 호출
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // 데이터
        Map<String, Object> attributes;
        String username;
        //String role = UserRoleType.ROLE_USER.name();
        String email;
        String nickname;
        String providerId;

        // provider 제공자별 데이터 획득 -> 네이버,구글 프로바이더에 따라 다름 -> 프로바이더가 추가될 때마다 소스코드를 수정해야하므로 추후에 개선 필요
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        if (registrationId.equals(SocialProviderType.NAVER.name())) {
            attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            username = registrationId + "_" + attributes.get("id");
            email = attributes.get("email").toString();
            nickname = attributes.get("nickname") != null
                    ? attributes.get("nickname").toString()
                    : "naver_user";
            providerId = attributes.get("id").toString();
        } else if (registrationId.equals(SocialProviderType.GOOGLE.name())) {
            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            username = registrationId + "_" + attributes.get("sub");
            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();
            providerId = attributes.get("sub").toString();
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // 데이터베이스 조회 -> 존재하면 업데이트, 없으면 신규 가입
        Optional<UserEntity> optionalUser = userRepository.findByUsernameAndIsSocial(username, true);
        UserEntity user;
        if (optionalUser.isPresent()) {
            // 기존 유저 조회
        	user = optionalUser.get();
            // 기존 유저 업데이트
            UserRequestDTO dto = new UserRequestDTO();
            dto.setNickname(nickname);
            dto.setEmail(email);
            user.updateUser(dto);
        } else {
            // 기본 ROLE_USER 조회
            RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalArgumentException("기본 권한이 존재하지 않습니다."));
            // 신규 유저 추가
            user = UserEntity.builder()
                    .username(username)
                    .nickname(nickname)
                    .email(email)
                    .password("")
                    .locked(false)
                    .enabled(true)
                    .isSocial(true)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .providerId(providerId)
                    .build();
            
            user.addRole(userRole);
        }

        // 저장
        //userRepository.save(user);
        
        List<GrantedAuthority> authorities =
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName()))
                        .collect(Collectors.toList());
        
        return new CustomOAuth2User(attributes, authorities, username);
    }
    
    
    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDTO readUser() {
        
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 여부 먼저 확인
        if (authentication == null || !authentication.isAuthenticated()|| authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String username = authentication.getName();
        
        // 유저 존재 여부 확인
        UserEntity entity = userRepository.findByUsernameAndLocked(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }
    
    // 관리자용 전체 유저 조회 
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
    	return userRepository.findAll()
    	        .stream()
    	        .map(user -> new UserResponseDTO(
    	                user.getUsername(),
    	                user.getIsSocial(),
    	                user.getNickname(),
    	                user.getEmail()
    	        ))
    	        .toList();
    }
    
}
