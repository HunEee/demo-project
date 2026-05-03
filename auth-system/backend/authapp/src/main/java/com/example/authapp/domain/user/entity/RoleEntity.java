package com.example.authapp.domain.user.entity;

import java.util.Set;
import java.util.HashSet;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // ROLE_USER, ROLE_ADMIN
    
    @Builder.Default
    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();
    
    
}
