package com.example.authapp.domain.user.entity;

public enum UserRoleType {
	ROLE_ADMIN("관리자"),
	ROLE_USER("사용자");

	private final String label;

	UserRoleType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
	
}
