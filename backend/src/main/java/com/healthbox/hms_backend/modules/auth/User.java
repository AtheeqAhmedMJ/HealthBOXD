package com.healthbox.hms_backend.modules.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "phno")
    private String phno;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "hospital_id") // null only for SUPER_ADMIN
    private Long hospitalId;

    @Column(name = "razorpay_account_id", length = 40)
    private String razorpayAccountId;
}
