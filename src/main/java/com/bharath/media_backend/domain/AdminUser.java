package com.bharath.media_backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="admin_users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUser {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true, nullable=false)
    private String email;

    @Column(nullable=false)
    private String hashedPassword;

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private String role; // ADMIN
}
