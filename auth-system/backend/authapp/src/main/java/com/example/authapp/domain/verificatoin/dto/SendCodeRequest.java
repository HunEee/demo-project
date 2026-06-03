package com.example.authapp.domain.verificatoin.dto;

import com.example.authapp.domain.verificatoin.entity.EmailCodePurpose;

public record SendCodeRequest(
	    String email,
	    String username,
	    EmailCodePurpose purpose
) {}
