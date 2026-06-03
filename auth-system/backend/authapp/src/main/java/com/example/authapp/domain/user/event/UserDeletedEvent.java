package com.example.authapp.domain.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDeletedEvent {

    private final String username;
    
}
