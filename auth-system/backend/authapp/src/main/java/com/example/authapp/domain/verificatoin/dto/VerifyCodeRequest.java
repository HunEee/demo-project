package com.example.authapp.domain.verificatoin.dto;

import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;

public record VerifyCodeRequest(
	    String email,
	    String code,
	    EmailCodePurpose purpose
) {}
