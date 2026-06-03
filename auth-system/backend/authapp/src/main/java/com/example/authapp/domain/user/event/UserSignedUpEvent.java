package com.example.authapp.domain.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSignedUpEvent {

    private final Long userId;
    private final String username;
    private final String email;
    
}
