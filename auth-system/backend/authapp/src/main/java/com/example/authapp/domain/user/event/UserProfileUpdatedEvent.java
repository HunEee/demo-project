package com.example.authapp.domain.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileUpdatedEvent {

    private final String username;
    
}
