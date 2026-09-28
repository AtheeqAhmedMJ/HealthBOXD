package com.healthbox.hms_backend.modules.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User saveUser(String phno, String username, String password, String email, Role role, Long hospitalId) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        User user = User.builder()
                .phno(phno)
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role(role)
                .hospitalId(hospitalId)
                .build();
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByPhone(String phone) {
        return userRepository.findById(phone).orElse(null);
    }
}
