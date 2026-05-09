package com.example.authapp.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	    // 아이디: 영문+숫자, 4~20자
	    @NotBlank(message = "아이디는 필수입니다.")
	    @Size(min = 4, max = 20, message = "아이디는 4~20자입니다.")
	    @Pattern(
	        regexp = "^[a-zA-Z0-9]+$",
	        message = "아이디는 영문과 숫자만 가능합니다."
	    )
	    String username,

	    // 비밀번호: 영문+숫자+특수문자 포함
	    @NotBlank(message = "비밀번호는 필수입니다.")
	    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
	    @Pattern(
	        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).+$",
	        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	    )
	    String password,

	    // 이메일
	    @NotBlank(message = "이메일은 필수입니다.")
	    @Email(message = "올바른 이메일 형식이 아닙니다.")
	    String email,

	    // 인증코드
	    @NotBlank(message = "인증코드는 필수입니다.")
	    @Pattern(
	        regexp = "^[0-9]{6}$",
	        message = "인증코드는 6자리 숫자입니다."
	    )
	    String verificationCode,

	    // 닉네임
	    @NotBlank(message = "닉네임은 필수입니다.")
	    @Size(min = 2, max = 10, message = "닉네임은 2~10자입니다.")
	    String nickname,

	    // 선택
	    String profileImage
) {}
